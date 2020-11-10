package io.nextpos.einvoice;

import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("prod")
@Disabled
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration")
class EinvoiceServiceApplicationTests {

	private final ElectronicInvoiceRepository electronicInvoiceRepository;

	@Autowired
	EinvoiceServiceApplicationTests(ElectronicInvoiceRepository electronicInvoiceRepository) {
		this.electronicInvoiceRepository = electronicInvoiceRepository;
	}

	@Test
	void contextLoads() {

		electronicInvoiceRepository.findByInternalInvoiceNumber("CR33735465").ifPresent(inv -> {
			System.out.println(inv);
		});
	}

}
