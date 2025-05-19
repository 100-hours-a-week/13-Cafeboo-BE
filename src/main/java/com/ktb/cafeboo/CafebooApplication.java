package com.ktb.cafeboo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class CafebooApplication {

	public static void main(String[] args) {
		SpringApplication.run(CafebooApplication.class, args);
	}

}
