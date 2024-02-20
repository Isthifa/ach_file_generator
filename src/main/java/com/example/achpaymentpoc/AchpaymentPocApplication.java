package com.example.achpaymentpoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "ACH Payment POC", version = "1.0", description = "ACH Payment POC"))
public class AchpaymentPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(AchpaymentPocApplication.class, args);
	}

}
