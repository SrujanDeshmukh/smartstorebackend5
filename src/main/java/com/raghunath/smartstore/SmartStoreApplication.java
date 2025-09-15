package com.raghunath.smartstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
@EnableAsync
public class SmartStoreApplication {
	public static void main(String[] args) {
		SpringApplication.run(SmartStoreApplication.class, args);
	}
}
