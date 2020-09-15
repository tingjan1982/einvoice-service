package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import org.apache.commons.io.FileUtils;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EInvoiceMessageServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageServiceImplTest.class);

    private final EInvoiceMessageService eInvoiceMessageService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final TurnkeyConfigProperties turnkeyConfigProperties;

    @Autowired
    EInvoiceMessageServiceImplTest(EInvoiceMessageService eInvoiceMessageService, ElectronicInvoiceRepository electronicInvoiceRepository, TurnkeyConfigProperties turnkeyConfigProperties) {
        this.eInvoiceMessageService = eInvoiceMessageService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.turnkeyConfigProperties = turnkeyConfigProperties;
    }

    @BeforeEach
    void prepare() {
        final File file = new File(turnkeyConfigProperties.getB2c().getCreateInvoiceDir());

        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @AfterEach
    void teardown() throws Exception {
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2c().getCreateInvoiceDir()));
    }

    @Test
    void createEInvoice() {

        final ElectronicInvoice electronicInvoice = new ElectronicInvoice(ObjectId.get().toString(),
                "AG-12345678",
                new ElectronicInvoice.InvoicePeriod(ZoneId.of("Asia/Taipei")),
                new BigDecimal("150"),
                new BigDecimal("7.5"),
                "83515813",
                "Rain App",
                List.of(new ElectronicInvoice.InvoiceItem("coffee", 1, new BigDecimal("150"), new BigDecimal("150"))));
        electronicInvoiceRepository.save(electronicInvoice);

        final ElectronicInvoice updatedEInvoice = eInvoiceMessageService.createEInvoice(electronicInvoice.getId());

        assertThat(updatedEInvoice.getInvoiceStatus()).isEqualByComparingTo(ElectronicInvoice.InvoiceStatus.MIG_CREATED);
    }
}