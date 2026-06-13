package com.fifaworldcup.Fifa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FifaApplication {

	public static void main(String[] args) {
		SpringApplication.run(FifaApplication.class, args);
	}

}
