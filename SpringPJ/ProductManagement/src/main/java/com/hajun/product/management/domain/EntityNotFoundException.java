package com.hajun.product.management.domain;

public class EntityNotFoundException extends RuntimeException{
	public EntityNotFoundException(String message) {
		super(message);
	}
}
