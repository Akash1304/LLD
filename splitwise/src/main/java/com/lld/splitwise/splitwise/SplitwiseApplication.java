package com.lld.splitwise.splitwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class SplitwiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(SplitwiseApplication.class, args);
	}

}
