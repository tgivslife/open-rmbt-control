package com.rtr.nettest.controller;

import com.rtr.nettest.TestUtils;
import com.rtr.nettest.advice.RtrAdvice;
import com.rtr.nettest.request.NewsParametersRequest;
import com.rtr.nettest.response.NewsListResponse;
import com.rtr.nettest.response.NewsResponse;
import com.rtr.nettest.service.NewsListService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.rtr.nettest.TestConstants.*;
import static com.rtr.nettest.constant.URIConstants.NEWS_URL;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class NewsListControllerTest {
    private MockMvc mockMvc;

    @MockBean
    private NewsListService newsListService;

    @Before
    public void setUp() {
        NewsListController newsListController = new NewsListController(newsListService);
        mockMvc = MockMvcBuilders.standaloneSetup(newsListController)
                .setControllerAdvice(new RtrAdvice())
                .build();
    }

    @Test
    public void showNewsList_whenCommonRequest_expectGetAllNewsCalled() throws Exception {
        var request = getNewsParametersRequest();
        var response = getNewsListResponse();
        when(newsListService.getAllNews(request)).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post(NEWS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.news", hasSize(1)))
                .andExpect(jsonPath("$.news[0].uid").value(DEFAULT_UID))
                .andExpect(jsonPath("$.news[0].title").value(DEFAULT_NEWS_TITLE))
                .andExpect(jsonPath("$.news[0].text").value(DEFAULT_NEWS_TEXT));

        verify(newsListService).getAllNews(request);
    }

    private NewsListResponse getNewsListResponse() {
        return new NewsListResponse(List.of(getNewsResponse()));
    }

    private NewsResponse getNewsResponse() {
        return NewsResponse.builder()
                .uid(DEFAULT_UID)
                .text(DEFAULT_NEWS_TEXT)
                .title(DEFAULT_NEWS_TITLE)
                .build();
    }

    private NewsParametersRequest getNewsParametersRequest() {
        return NewsParametersRequest.builder()
                .language(DEFAULT_LANGUAGE)
                .lastNewsUid(DEFAULT_LAST_NEWS_UID)
                .platform(DEFAULT_PLATFORM)
                .softwareVersionCode(DEFAULT_SOFTWARE_VERSION_CODE)
                .uuid(DEFAULT_NEWS_UUID)
                .build();
    }
}