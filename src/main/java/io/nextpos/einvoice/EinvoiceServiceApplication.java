package io.nextpos.einvoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
public class EinvoiceServiceApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(EinvoiceServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(EinvoiceServiceApplication.class, args);
	}

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));

		LOGGER.info("Configured locale: {}, timezone: {}", Locale.getDefault(), TimeZone.getDefault());
		LOGGER.info("Date in configured timezone: {}", new Date());
	}
}
