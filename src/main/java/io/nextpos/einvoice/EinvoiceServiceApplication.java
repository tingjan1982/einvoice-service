package io.nextpos.einvoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EinvoiceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EinvoiceServiceApplication.class, args);
	}

}
