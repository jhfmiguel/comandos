package com.comandos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.comandos", "com.fariamiguel.app"})
@EntityScan(basePackages = {"com.comandos", "com.fariamiguel.app"})
@EnableJpaRepositories(basePackages = {"com.comandos", "com.fariamiguel.app"})
public class ComandosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComandosApiApplication.class, args);
	}

}
