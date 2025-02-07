//데이터베이스 없이 컬렉션에만 저장된 데이터를 조회


package com.hajun.product.management.infrastructure;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hajun.product.management.domain.EntityNotFoundException;
import com.hajun.product.management.domain.Product;
import com.hajun.product.management.domain.ProductRepository;


// profile(test) : 개발자가 로컬 개발환경에서 기능을 테스트할 때 사용하는 환경
@Repository
@Profile("test")
public class ListProductRepository implements ProductRepository{
	
	private List<Product> products = new CopyOnWriteArrayList<>();
	private AtomicLong sequence = new AtomicLong(1L);
	
	public Product add(Product product) {
		product.setId(sequence.getAndAdd(1L));
		products.add(product);
		return product;
	}
	
	public Product findById(Long id) {
		return products.stream().filter(product -> product.sameId(id)).findFirst()
				.orElseThrow(() -> new EntityNotFoundException("product를 찾지 못했습니다"));
	}
	
	public List<Product> findAll(){
		return products;
	}
	
	// 상품 이름에 포함된 문자열로 검색하는 기능
	public List<Product> findByNameContaining(String name){
		return products.stream()
				.filter(product -> product.containsName(name))
				.toList();
	}
	
	// 상품 수정
	public Product update(Product product){
		Integer indexToModify = products.indexOf(product);
		products.set(indexToModify, product);
		return product;
	}
	
	// 상품 삭제
	// findById : 위에서 선언한 메서드를 재사용(레이어드 아키텍처)
	public void delete(Long id) {
		Product product = this.findById(id);
		products.remove(product);
	}
}
