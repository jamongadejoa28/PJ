package com.hajun.product.management.infrastructure;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.hajun.product.management.domain.Product;
import com.hajun.product.management.domain.ProductRepository;

// profile(prod) : 개발자가 아니라 사용자에게 서비스가 제공된다는 의미
@Repository
@Profile("prod")
public class DatabaseProductRepository implements ProductRepository{

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public DatabaseProductRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public Product add(Product product) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		SqlParameterSource namedParameter = new BeanPropertySqlParameterSource(product);

		namedParameterJdbcTemplate.update("INSERT INTO products (name,price,amount) VALUES (:name, :price, :amount)",
				namedParameter, keyHolder);

		Long generateId = keyHolder.getKey().longValue();
		product.setId(generateId);

		return product;
	}

	public Product findById(Long id) {
		SqlParameterSource namedParameter = new MapSqlParameterSource("id", id);

		Product product = namedParameterJdbcTemplate.queryForObject(
				"SELECT id, name, price, amount FROM products WHERE id=:id", namedParameter,
				new BeanPropertyRowMapper<>(Product.class));

		return product;
	}

	public List<Product> findAll() {
		List<Product> products = namedParameterJdbcTemplate.query("SELECT * FROM products",
				new BeanPropertyRowMapper<>(Product.class));
		return products;
	}

	public List<Product> findByNameContaining(String name) {
		SqlParameterSource namedParameter = new MapSqlParameterSource("name", "%" + name + "%");

		List<Product> products = namedParameterJdbcTemplate.query("SELECT * FROM products WHERE name LIKE :name",
				namedParameter, new BeanPropertyRowMapper<>(Product.class));

		return products;
	}

	public Product update(Product product) {
		SqlParameterSource namedParameter = new BeanPropertySqlParameterSource(product);

		namedParameterJdbcTemplate.update("UPDATE products SET name=:name, price=:price, amount=:amount WHERE id=:id",
				namedParameter);

		return product;
	}

	public void delete(Long id) {
		SqlParameterSource namedParameter = new MapSqlParameterSource("id", id);

		namedParameterJdbcTemplate.update("DELETE FROM products WHERE id=:id", namedParameter);
	}
}
