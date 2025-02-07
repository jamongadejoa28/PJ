package com.hajun.product.management.application;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@Service
@Validated
public class ValidationService {
	public <T> void checkValid(@Valid T validationTarget) {
		// do nothing
		// checkValid로 인자를 담아 호출하는 것만으로 유효성 검증이 이루어짐
	}
}
