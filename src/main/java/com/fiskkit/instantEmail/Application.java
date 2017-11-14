package com.fiskkit.instantEmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger("com.fiskkit.instantEmail.Application");

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
