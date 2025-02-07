package com.hajun.product.management.presentation;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hajun.product.management.application.SimpleProductService;

import jakarta.validation.Valid;

@RestController
public class ProductController {
	
	private SimpleProductService simpleProductService;
	
	public ProductController(SimpleProductService simpleProductService) {
		// TODO Auto-generated constructor stub
		this.simpleProductService = simpleProductService;
	}	
	
	@PostMapping("/products")
	public ProductDto createProduct(@Valid @RequestBody ProductDto productDto) {
		return simpleProductService.add(productDto);
	}
	
	// id로 조회
	@GetMapping("/products/{id}")
	public ProductDto findProductById(@PathVariable Long id) {
		return simpleProductService.findById(id);
	}
	
//  // 상품목록 전체 조회	
//	@GetMapping("/products")
//	public List<ProductDto> findAllProduct(){
//		return simpleProductService.findAll();
//	}
	
	// 전체목록 조회 or 문자열 포함 상품 조회
	@GetMapping("/products")
	public List<ProductDto> findProducts(@RequestParam(required = false) String name){
		if (null == name) {
			return simpleProductService.findAll();
		}
		return simpleProductService.findByNameContaining(name);
	}
	
	// 상품 수정
	@PutMapping("/products/{id}")
	public ProductDto updateProduct(@PathVariable Long id,
			@RequestBody ProductDto productDto) {
		productDto.setId(id);
		return simpleProductService.update(productDto);
	}
	
	// 상품 삭제
	// 이미 삭제된 상품으로 응답을 줬을 때 의미가 없어 반환 타입을 void로 선언
	// 상품추가와 상품수정도 void로 선언해도 무방하나 클라이언트 측에서 정보 확인이 필요할 수도 있어서 타입 반환
	@DeleteMapping("/products/{id}")
	public void deleteProduct(@PathVariable Long id) {
		simpleProductService.delete(id);
	}
}
