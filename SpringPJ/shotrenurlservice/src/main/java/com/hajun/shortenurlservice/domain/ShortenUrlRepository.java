package com.hajun.shortenurlservice.domain;

public interface ShortenUrlRepository {
	void saveShortenUrl(ShortenUrl shortenUrl);
	ShortenUrl findShortenUrlByShortenUrlKey(String shortenUrlKey);
}
