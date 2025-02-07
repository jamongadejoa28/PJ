package com.hajun.shortenurlservice.domain;

import java.util.Random;

public class ShortenUrl {
	private String originalUrl;
	private String shortenUrlKey;
	private Long redirectCount;
	
	// Base56 문자열 생성 메서드
	public static String generateShortenUrlKey() {
		String base56Characters = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
        Random random = new Random();
        StringBuilder shortenUrlKey = new StringBuilder();

        for(int count = 0; count < 8; count++) {
            int base56CharactersIndex = random.nextInt(0, base56Characters.length());
            char base56Character = base56Characters.charAt(base56CharactersIndex);

            shortenUrlKey.append(base56Character);
        }

        return shortenUrlKey.toString();
	}
	
	public ShortenUrl(String originalUrl, String shortenUrlKey) {
		this.originalUrl = originalUrl;
		this.shortenUrlKey = shortenUrlKey;
		this.redirectCount = 0L;
	}
	
	public void increaseRedirectCount() {
		this.redirectCount += 1;
	}
	
	public String getOriginalUrl() {
		return originalUrl;
	}
	public String getShortenUrlKey() {
		return shortenUrlKey;
	}
	public Long getRedirectCount() {
		return redirectCount;
	}
	public void setOriginalUrl(String originalUrl) {
		this.originalUrl = originalUrl;
	}
	public void setShortenUrlKey(String shortenUrlKey) {
		this.shortenUrlKey = shortenUrlKey;
	}
	public void setRedirectCount(Long redirectCount) {
		this.redirectCount = redirectCount;
	}
}	
