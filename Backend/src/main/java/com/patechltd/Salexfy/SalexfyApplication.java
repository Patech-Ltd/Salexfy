package com.patechltd.Salexfy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class SalexfyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalexfyApplication.class, args);
	}

}
