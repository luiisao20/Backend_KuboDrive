package com.luisdev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KubodriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(KubodriveApplication.class, args);
	}

}
