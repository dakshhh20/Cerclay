package com.mittiandmore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MittiAndMoreBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MittiAndMoreBackendApplication.class, args);
	}

}
