package com.hajun.shortenurlservice.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hajun.shortenurlservice.application.SimpleShortenUrlService;

@WebMvcTest(controllers = ShortenUrlRestController.class)
class ShortenUrlRestControllerTest {

	// Spring Boot 3.4.0부터 MockBean이 MockitoBean으로 대체됨
	@MockitoBean
	private SimpleShortenUrlService simpleShortenUrlService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("원래의 URL로 리다이렉트 되어야한다")
	void redirectTest() throws Exception {
		String expectedOriginalUrl = "https://www.google.com/";

		when(simpleShortenUrlService.getOriginalUrlByShortenUrlKey(any())).thenReturn(expectedOriginalUrl);

		mockMvc.perform(get("/any-key")).andDo(print()).andExpect(status().isTemporaryRedirect())
				.andExpect(header().string("Location", expectedOriginalUrl));
	}

}
