package com.banking.ledger.Reconcilation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReconcilationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReconcilationApplication.class, args);
	}

}
