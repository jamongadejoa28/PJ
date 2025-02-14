package com.hajun.product.management;

import java.sql.Connection;

import javax.sql.DataSource;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class ProductManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductManagementApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
				.setFieldMatchingEnabled(true);
		
		return modelMapper;
	}
	
	@Bean
	@Profile("prod")
	public ApplicationRunner runner(DataSource dataSource) {
		return args -> {
			Connection connection = dataSource.getConnection();
		};
	}

}
