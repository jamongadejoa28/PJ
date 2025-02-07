package com.hajun.product.management.domain;

import java.util.Objects;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class Product {
	private Long id;

	@Size(min = 1, max = 100)
	private String name;

	@Max(1_000_000)
	@Min(0)
	private Integer price;

	@Max(9_999)
	@Min(0)
	private Integer amount;

	public Product() {
	}

	public Product(Long id, String name, Integer price, Integer amount) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.amount = amount;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Integer getPrice() {
		return price;
	}

	public Integer getAmount() {
		return amount;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrice(Integer price) {
		this.price = price;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

	public Boolean sameId(Long id) {
		return this.id.equals(id);
	}

	public Boolean containsName(String name) {
		return this.name.contains(name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Product other = (Product) obj;
		return Objects.equals(id, other.id);
	}

}
