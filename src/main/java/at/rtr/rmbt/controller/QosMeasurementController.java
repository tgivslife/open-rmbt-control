package at.rtr.rmbt.controller;

import at.rtr.rmbt.request.QosResultRequest;
import at.rtr.rmbt.response.ErrorResponse;
import at.rtr.rmbt.response.MeasurementQosResponse;
import at.rtr.rmbt.service.QosMeasurementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static at.rtr.rmbt.constant.URIConstants.MEASUREMENT_QOS_REQUEST;
import static at.rtr.rmbt.constant.URIConstants.RESULT_QOS_URL;

@Api("Qos measurement")
@RestController
@RequiredArgsConstructor
public class QosMeasurementController {

    private final QosMeasurementService qosMeasurementService;

    @ApiOperation("Provide parameters for qos measurements.")
    @PostMapping(MEASUREMENT_QOS_REQUEST)
    public MeasurementQosResponse provideMeasurementQosParameters(HttpServletRequest httpServletRequest, @RequestHeader Map<String, String> headers) {
        return qosMeasurementService.getQosParameters(httpServletRequest, headers);
    }

    @ApiOperation("Save QoS test results")
    @PostMapping(RESULT_QOS_URL)
    public ErrorResponse saveQosMeasurementResult(@RequestBody QosResultRequest qosResultRequest) {
        return qosMeasurementService.saveQosMeasurementResult(qosResultRequest);
    }
}
