package com.hajun.shortenurlservice.presentation;

import com.hajun.shortenurlservice.domain.ShortenUrl;

public class ShortenUrlInformationDto {
	private String originalUrl;
	private String shortenUrlKey;
	private Long redirectCount;
	
	public ShortenUrlInformationDto(ShortenUrl shortenUrl) {
		this.originalUrl = shortenUrl.getOriginalUrl();
		this.shortenUrlKey = shortenUrl.getShortenUrlKey();
		this.redirectCount = shortenUrl.getRedirectCount();
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
