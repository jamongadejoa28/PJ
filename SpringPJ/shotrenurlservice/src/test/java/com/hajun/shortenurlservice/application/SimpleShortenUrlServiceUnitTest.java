package com.hajun.shortenurlservice.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hajun.shortenurlservice.domain.LackOfShortenUrlKeyException;
import com.hajun.shortenurlservice.domain.ShortenUrl;
import com.hajun.shortenurlservice.domain.ShortenUrlRepository;
import com.hajun.shortenurlservice.presentation.ShortenUrlCreateRequestDto;

@ExtendWith(MockitoExtension.class)
class SimpleShortenUrlServiceUnitTest {

	@Mock
	private ShortenUrlRepository shortenUrlRepository;

	@InjectMocks
	private SimpleShortenUrlService simpleShortenUrlService;

	@Test
	@DisplayName("단축 URL이 계속 중복되면 LackOfShortenUrlKeyException 예외가 발생해야한다.")
	void throwLackOfShortenUrlKeyException() {
		ShortenUrlCreateRequestDto shortenUrlCreateRequestDto = new ShortenUrlCreateRequestDto(null);
		when(shortenUrlRepository.findShortenUrlByShortenUrlKey(any())).thenReturn(new ShortenUrl(null, null));

		Assertions.assertThrows(LackOfShortenUrlKeyException.class, () -> {
			simpleShortenUrlService.generateShortenUrl(shortenUrlCreateRequestDto);
		});
	}
}
