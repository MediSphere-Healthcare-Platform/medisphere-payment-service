package com.medisphere.payment;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MedispherePaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedispherePaymentServiceApplication.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Bean
	public Gson gson() {
		return new Gson();
	}

}
