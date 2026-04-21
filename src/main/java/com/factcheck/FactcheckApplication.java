package com.factcheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FactcheckApplication {

	public static void main(String[] args) {
		SpringApplication.run(FactcheckApplication.class, args);
	}

}
