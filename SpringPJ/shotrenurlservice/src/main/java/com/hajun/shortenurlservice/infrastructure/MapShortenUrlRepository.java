package com.hajun.shortenurlservice.infrastructure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hajun.shortenurlservice.domain.ShortenUrl;
import com.hajun.shortenurlservice.domain.ShortenUrlRepository;

@Repository
public class MapShortenUrlRepository implements ShortenUrlRepository{
	
	private Map<String, ShortenUrl> shortenUrls = new ConcurrentHashMap<>();
	
	@Override
	public void saveShortenUrl(ShortenUrl shortenUrl) {
		shortenUrls.put(shortenUrl.getShortenUrlKey(), shortenUrl);
	}
	
	@Override
	public ShortenUrl findShortenUrlByShortenUrlKey(String shortenUrlKey) {
		ShortenUrl shortenUrl = shortenUrls.get(shortenUrlKey);
		return shortenUrl;
	}
}
