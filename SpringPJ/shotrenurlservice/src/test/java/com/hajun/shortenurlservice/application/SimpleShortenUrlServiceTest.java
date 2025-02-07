package com.hajun.shortenurlservice.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hajun.shortenurlservice.domain.NotFoundShortenUrlException;
import com.hajun.shortenurlservice.presentation.ShortenUrlCreateRequestDto;
import com.hajun.shortenurlservice.presentation.ShortenUrlCreateResponseDto;

@SpringBootTest
class SimpleShortenUrlServiceTest {

	@Autowired
	private SimpleShortenUrlService simpleShortenUrlService;
	
	@Test
	@DisplayName("URL을 단축한 후 단축된 URL 키로 조회하면 원래 URL이 조회되어야 한다.")
	void shortenUrlAddTest() {
		String expectedOriginalUrl = "https://www.google.com/";
		ShortenUrlCreateRequestDto shortenUrlCreateRequestDto = new ShortenUrlCreateRequestDto(expectedOriginalUrl);
		
		ShortenUrlCreateResponseDto shortenUrlCreateResponseDto = simpleShortenUrlService.generateShortenUrl(shortenUrlCreateRequestDto);
		String shortenUrlKey = shortenUrlCreateResponseDto.getShortenUrlKey();
		String originalUrl = simpleShortenUrlService.getOriginalUrlByShortenUrlKey(shortenUrlKey);
		assertTrue(originalUrl.equals(expectedOriginalUrl));
	}
	
	@Test
	@DisplayName("존재하지 않는 단축URL을 조회하면 NotFoundShortenUrlException이 발생해야 한다.")
	void findNotExistUrlTest() {
		String testUrl = "https://www.voidUrl.com/";
		
		assertThrows(NotFoundShortenUrlException.class, () -> {
			simpleShortenUrlService.getOriginalUrlByShortenUrlKey(testUrl);
		});
	}

}
