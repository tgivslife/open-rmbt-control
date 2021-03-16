package at.rtr.rmbt.service.impl;

import at.rtr.rmbt.constant.Constants;
import at.rtr.rmbt.constant.ErrorMessage;
import at.rtr.rmbt.enums.TestStatus;
import at.rtr.rmbt.exception.NotSupportedClientVersionException;
import at.rtr.rmbt.exception.TestNotFoundException;
import at.rtr.rmbt.mapper.TestMapper;
import at.rtr.rmbt.model.Test;
import at.rtr.rmbt.properties.ApplicationProperties;
import at.rtr.rmbt.repository.NetworkTypeRepository;
import at.rtr.rmbt.repository.TestRepository;
import at.rtr.rmbt.request.ResultRequest;
import at.rtr.rmbt.service.*;
import at.rtr.rmbt.utils.HelperFunctions;
import at.rtr.rmbt.utils.ValidateUtils;
import com.google.common.net.InetAddresses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final TestRepository testRepository;
    private final GeoLocationService geoLocationService;
    private final RadioCellService radioCellService;
    private final RadioSignalService radioSignalService;
    private final CellLocationService cellLocationService;
    private final SignalService signalService;
    private final NetworkTypeRepository networkTypeRepository;
    private final PingService pingService;
    private final SpeedService speedService;
    private final ApplicationProperties applicationProperties;
    private final TestMapper testMapper;

    private final static Pattern MCC_MNC_PATTERN = Pattern.compile("\\d{3}-\\d+");

    @Override
    @Transactional
    public void processResultRequest(HttpServletRequest httpServletRequest, ResultRequest resultRequest) {
        UUID requestUUID = UUID.fromString(resultRequest.getTestToken().split("_")[0]);

        Test test = testRepository.findByUuidOrOpenTestUuid(requestUUID)
                .orElseThrow(() -> new TestNotFoundException(String.format(ErrorMessage.TEST_NOT_FOUND, requestUUID)));
        verifyClientVersion(resultRequest);
        testMapper.updateTestWithResultRequest(resultRequest, test);
        test.setNetworkOperator(getOperator(resultRequest.getTelephonyNetworkOperator()));
        test.setNetworkSimOperator(getOperator(resultRequest.getTelephonyNetworkSimOperator()));
        setRMBTClientInfo(resultRequest, test);
        setSourceIp(httpServletRequest, test);
        processSpeedData(resultRequest, test);
        processPingData(resultRequest, test);
        processGeoLocation(resultRequest, test);
        processRadioInfo(resultRequest, test);
        processCellLocation(resultRequest, test);
        processSignals(resultRequest, test);
        setNetworkType(test);
        setAndroidPermission(resultRequest, test);
        setSpeedAndPing(resultRequest, test);
        setStatus(resultRequest, test);
        testRepository.save(test);
    }

    private void setStatus(ResultRequest resultRequest, Test test) {
        test.setStatus(TestStatus.FINISHED);
        if (Objects.nonNull(resultRequest.getTestStatus())) {
            switch (resultRequest.getTestStatus()) {
                case "0":
                case "SUCCESS":
                    test.setStatus(TestStatus.FINISHED);
                    break;
                case "1":
                case "ERROR":
                    test.setStatus(TestStatus.ERROR);
                    break;
                case "2":
                case "ABORTED":
                    test.setStatus(TestStatus.ABORTED);
                    break;
            }
        }
    }

    private void setNetworkType(Test test) {
        setMaxNetworkType(test);
        checkForDifferentType(test);

        if (test.getNetworkType() <= 0) {
            throw new IllegalArgumentException(ErrorMessage.ERROR_NETWORK_TYPE);
        }
    }

    private void setSpeedAndPing(ResultRequest resultRequest, Test test) {
        Optional.ofNullable(resultRequest.getDownloadSpeed())
                .filter(downloadSpeed -> downloadSpeed > Constants.MIN_SPEED)
                .filter(downloadSpeed -> downloadSpeed < Constants.MAX_SPEED)
                .ifPresentOrElse(test::setDownloadSpeed, () -> {
                    throw new IllegalArgumentException(ErrorMessage.ERROR_DOWNLOAD_INSANE);
                });

        Optional.ofNullable(resultRequest.getUploadSpeed())
                .filter(updloadSpeed -> updloadSpeed > Constants.MIN_SPEED)
                .filter(uploadSpeed -> uploadSpeed < Constants.MAX_SPEED)
                .ifPresentOrElse(test::setUploadSpeed, () -> {
                    throw new IllegalArgumentException(ErrorMessage.ERROR_UPLOAD_INSANE);
                });

        Optional.ofNullable(resultRequest.getPingShortest())
                .filter(ping -> ping > Constants.MIN_PING)
                .filter(ping -> ping < Constants.MAX_PING)
                .ifPresentOrElse(test::setShortestPing, () -> {
                    throw new IllegalArgumentException(ErrorMessage.ERROR_PING_INSANE);
                });
    }

    private void setAndroidPermission(ResultRequest resultRequest, Test test) {
        Optional.ofNullable(resultRequest.getAndroidPermissionStatuses())
                .ifPresent(test::setAndroidPermissions);
    }

    private void checkForDifferentType(Test test) {
        networkTypeRepository.findByOpenTestUUIDAndAggregate(test.getOpenTestUuid())
                .ifPresent(networkType -> {
                    test.setNetworkType(networkType.getUid());
                });
    }

    private void setMaxNetworkType(Test test) {
        networkTypeRepository.findTopByOpenTestUUIDAndOrderByTechnologyOrderDesc(test.getOpenTestUuid())
                .ifPresent(networkType -> {
                    test.setNetworkType(networkType.getUid());
                });
    }

    private void processSignals(ResultRequest resultRequest, Test test) {
        if (Objects.nonNull(resultRequest.getSignals())) {
            signalService.processSignalRequests(resultRequest.getSignals(), test);
        }
    }

    private void processCellLocation(ResultRequest resultRequest, Test test) {
        if (Objects.nonNull(resultRequest.getCellLocations())) {
            cellLocationService.saveCellLocationRequests(resultRequest.getCellLocations(), test);
        }
    }

    private void processGeoLocation(ResultRequest resultRequest, Test test) {
        if (Objects.nonNull(resultRequest.getGeoLocations())) {
            geoLocationService.processGeoLocationRequests(resultRequest.getGeoLocations(), test);
        }
    }

    private void processRadioInfo(ResultRequest resultRequest, Test test) {
        if (Objects.nonNull(resultRequest.getRadioInfo())) {
            radioCellService.processRadioCellRequests(resultRequest.getRadioInfo().getCells(), test);
            radioSignalService.saveRadioSignalRequests(resultRequest.getRadioInfo().getSignals(), test);
        }
    }

    private void processPingData(ResultRequest resultRequest, Test test) {
        if (Objects.nonNull(resultRequest.getPings())) {
            pingService.savePingRequests(resultRequest.getPings(), test);
        }
    }

    private void processSpeedData(ResultRequest resultRequest, Test test) {
        if (Objects.nonNull(resultRequest.getSpeedDetails())) {
            speedService.processSpeedRequests(resultRequest.getSpeedDetails(), test);
        }
    }

    private void setSourceIp(HttpServletRequest httpServletRequest, Test test) {
        InetAddress sourceAddress = InetAddresses.forString(httpServletRequest.getRemoteAddr());
        test.setSourceIp(InetAddresses.toAddrString(sourceAddress));
        test.setSourceIpAnonymized(HelperFunctions.anonymizeIp(sourceAddress));
    }

    private void setRMBTClientInfo(ResultRequest resultRequest, Test test) {
        Optional.ofNullable(resultRequest.getTestIpLocal())
                .ifPresent(ipLocalRaw -> {
                    InetAddress ipLocalAddress = InetAddresses.forString(ipLocalRaw);
                    test.setClientIpLocal(InetAddresses.toAddrString(ipLocalAddress));
                    test.setClientIpLocalAnonymized(HelperFunctions.anonymizeIp(ipLocalAddress));
                    test.setClientIpLocalType(HelperFunctions.IpType(ipLocalAddress));

                    Optional.ofNullable(test.getClientPublicIp())
                            .map(InetAddresses::forString)
                            .ifPresent(ipPublicAddress -> {
                                test.setNatType(HelperFunctions.getNatType(ipLocalAddress, ipPublicAddress));
                            });
                });

        Optional.ofNullable(resultRequest.getTestIpServer())
                .ifPresent(ipServer -> {
                    InetAddress testServerInetAddress = InetAddresses.forString(ipServer);
                    test.setServerIp(InetAddresses.toAddrString(testServerInetAddress));
                });
    }

    private String getOperator(String field) {
        return Optional.ofNullable(field)
                .filter(x -> MCC_MNC_PATTERN.matcher(x).matches())
                .orElse(null);
    }

    private void verifyClientVersion(ResultRequest resultRequest) {
        ValidateUtils.validateClientVersion(applicationProperties.getVersion(), resultRequest.getClientVersion());

        if (!applicationProperties.getClientNames().contains(resultRequest.getClientName().getLabel())) {
            throw new NotSupportedClientVersionException();
        }
    }
}