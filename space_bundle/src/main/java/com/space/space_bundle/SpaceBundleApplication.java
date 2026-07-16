package com.space.space_bundle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.space.space_bundle")
@EnableAsync
@EnableScheduling
public class SpaceBundleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpaceBundleApplication.class, args);
	}

}
