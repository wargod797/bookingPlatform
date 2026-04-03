package com.example.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class BookingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingPlatformApplication.class, args);
	}

}
