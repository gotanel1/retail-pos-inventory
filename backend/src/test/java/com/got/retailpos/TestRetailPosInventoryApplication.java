package com.got.retailpos;

import org.springframework.boot.SpringApplication;

public class TestRetailPosInventoryApplication {

	public static void main(String[] args) {
		SpringApplication.from(RetailPosInventoryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
