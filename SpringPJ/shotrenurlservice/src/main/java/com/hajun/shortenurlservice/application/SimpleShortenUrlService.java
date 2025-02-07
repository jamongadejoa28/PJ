package com.hajun.shortenurlservice.application;

import org.springframework.stereotype.Service;

import com.hajun.shortenurlservice.domain.LackOfShortenUrlKeyException;
import com.hajun.shortenurlservice.domain.NotFoundShortenUrlException;
import com.hajun.shortenurlservice.domain.ShortenUrl;
import com.hajun.shortenurlservice.domain.ShortenUrlRepository;
import com.hajun.shortenurlservice.presentation.ShortenUrlCreateRequestDto;
import com.hajun.shortenurlservice.presentation.ShortenUrlCreateResponseDto;
import com.hajun.shortenurlservice.presentation.ShortenUrlInformationDto;

@Service
public class SimpleShortenUrlService {

	private ShortenUrlRepository shortenUrlRepository;

	public SimpleShortenUrlService(ShortenUrlRepository shortenUrlRepository) {
		// TODO Auto-generated constructor stub
		this.shortenUrlRepository = shortenUrlRepository;
	}

	public ShortenUrlCreateResponseDto generateShortenUrl(ShortenUrlCreateRequestDto shortenUrlCreateRequestDto) {
		// 1. 단축 URL Key 생성
		String originalUrl = shortenUrlCreateRequestDto.getOriginalUrl();
		String shortenUrlKey = getUniqueShortenUrlKey();

		// 2. 원래의 URL과 단축 URL 키를 통해 ShortenUrl 도메인 객체 생성
		ShortenUrl shortenUrl = new ShortenUrl(originalUrl, shortenUrlKey);
		// 3. 생성된 ShortenUrl을 레포지토리를 통해 저장
		shortenUrlRepository.saveShortenUrl(shortenUrl);
		// 4. ShortenUrl을 ShortenUrlCreateResponseDto로 변환하여 반환
		ShortenUrlCreateResponseDto shortenUrlCreateResponseDto = new ShortenUrlCreateResponseDto(shortenUrl);

		return shortenUrlCreateResponseDto;
	}
	
	public String getUniqueShortenUrlKey() {
		final int MAX_RETRY_COUNT = 5; // 중복 검사 최대 횟수(자원한정)
		int count = 0;
		
		while(count++ < MAX_RETRY_COUNT) {
			String shortenUrlKey = ShortenUrl.generateShortenUrlKey();
			ShortenUrl shortenUrl = shortenUrlRepository.findShortenUrlByShortenUrlKey(shortenUrlKey);
			
			if(null == shortenUrl) return shortenUrlKey; 
		}
		throw new LackOfShortenUrlKeyException();
	}
	
	public String getOriginalUrlByShortenUrlKey(String shortenUrlKey) {
		ShortenUrl shortenUrl = shortenUrlRepository.findShortenUrlByShortenUrlKey(shortenUrlKey);
		if(null == shortenUrl) throw new NotFoundShortenUrlException();
		shortenUrl.increaseRedirectCount();
		shortenUrlRepository.saveShortenUrl(shortenUrl);
		String originalUrl = shortenUrl.getOriginalUrl();
		
		return originalUrl;
	}
	
	public ShortenUrlInformationDto getShortenUrlInformationByShortenUrlKey(String shortenUrlKey) {
		ShortenUrl shortenUrl = shortenUrlRepository.findShortenUrlByShortenUrlKey(shortenUrlKey);
		if(null == shortenUrl) throw new NotFoundShortenUrlException();
		ShortenUrlInformationDto shortenUrlInformationDto = new ShortenUrlInformationDto(shortenUrl);

		return shortenUrlInformationDto;
	}
}
