package com.comandos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.comandos")
@EnableJpaRepositories(basePackages = "com.comandos")
public class ComandosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComandosApiApplication.class, args);
	}

}
