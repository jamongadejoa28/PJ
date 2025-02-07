/*1. 단축 URL 생성 API -> /shortenUrl, POST
2. 단축 URL 리다이렉트 API -> /{shortenUrlKey}, GET
3. 단축 URL 정보 조회 API -> /shortenUrl/{shortenUrlKey}, GET*/

package com.hajun.shortenurlservice.presentation;


import java.net.URISyntaxException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hajun.shortenurlservice.application.SimpleShortenUrlService;

import jakarta.validation.Valid;

@RestController
public class ShortenUrlRestController {

	private SimpleShortenUrlService simpleShortenUrlService;

	ShortenUrlRestController(SimpleShortenUrlService simpleShortenUrlService) {
		this.simpleShortenUrlService = simpleShortenUrlService;
	}

	@PostMapping("/shortenUrl")
	public ResponseEntity<ShortenUrlCreateResponseDto> createShortUrl(
			@Valid @RequestBody ShortenUrlCreateRequestDto shortenUrlCreateRequestDto) {
		ShortenUrlCreateResponseDto shortenUrlCreateResponseDto = simpleShortenUrlService
				.generateShortenUrl(shortenUrlCreateRequestDto);
		return ResponseEntity.ok(shortenUrlCreateResponseDto);
	}

	@GetMapping("/{shortenUrlKey}")
	public ResponseEntity<?> redirectShortenUrl(@PathVariable String shortenUrlKey) throws URISyntaxException {
		String originalUrl = simpleShortenUrlService.getOriginalUrlByShortenUrlKey(shortenUrlKey);

		return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
				.header(HttpHeaders.LOCATION, originalUrl).build();
	}

	@GetMapping("/shortenUrl/{shortenUrlKey}")
	public ResponseEntity<ShortenUrlInformationDto> getShortenUrlInformation(@PathVariable String shortenUrlKey) {
		ShortenUrlInformationDto shortenUrlInformationDto = simpleShortenUrlService
				.getShortenUrlInformationByShortenUrlKey(shortenUrlKey);
		return ResponseEntity.ok(shortenUrlInformationDto);
	}
}
