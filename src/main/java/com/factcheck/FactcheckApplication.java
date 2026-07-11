package com.factcheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling   // stuck 분석 재조정 스위퍼(@Scheduled) 활성화
public class FactcheckApplication {

	public static void main(String[] args) {
		SpringApplication.run(FactcheckApplication.class, args);
	}

}
