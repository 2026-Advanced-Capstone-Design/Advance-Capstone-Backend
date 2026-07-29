package com.factcheck;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// test 프로파일(H2)로 띄운다. 운영 프로파일은 ddl-auto: create 라 로컬 MySQL 스키마를 날린다.
@SpringBootTest
@ActiveProfiles("test")
class FactcheckApplicationTests {

	@Test
	void contextLoads() {
	}

}
