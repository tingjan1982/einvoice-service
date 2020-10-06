package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
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

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final TurnkeyConfigProperties turnkeyConfigProperties;

    @Autowired
    EInvoiceMessageServiceImplTest(EInvoiceMessageService eInvoiceMessageService, PendingEInvoiceQueueService pendingEInvoiceQueueService, ElectronicInvoiceRepository electronicInvoiceRepository, InvoiceNumberRangeService invoiceNumberRangeService, TurnkeyConfigProperties turnkeyConfigProperties) {
        this.eInvoiceMessageService = eInvoiceMessageService;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.turnkeyConfigProperties = turnkeyConfigProperties;
    }

    @BeforeEach
    void prepare() {
        createDirectory(turnkeyConfigProperties.getB2c().getCreateInvoiceDir());
        createDirectory(turnkeyConfigProperties.getB2c().getVoidInvoiceDir());
        createDirectory(turnkeyConfigProperties.getB2p().getUnusedInvoiceNumberDir());
    }

    private void createDirectory(String directoryPath) {
        final File file = new File(directoryPath);

        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @AfterEach
    void teardown() throws Exception {
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2c().getCreateInvoiceDir()));
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2c().getVoidInvoiceDir()));
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2p().getUnusedInvoiceNumberDir()));
    }

    @Test
    void createEInvoice() {

        final ElectronicInvoice electronicInvoice = createMockElectronicInvoice();

        final PendingEInvoiceQueue pendingEInvoiceQueue = pendingEInvoiceQueueService.createPendingEInvoiceQueue(electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE);

        final ElectronicInvoice updatedEInvoice = eInvoiceMessageService.createElectronicInvoiceMIG(pendingEInvoiceQueue);

        assertThat(updatedEInvoice.getInvoiceStatus()).isEqualByComparingTo(ElectronicInvoice.InvoiceStatus.MIG_CREATED);
        assertThat(new File(turnkeyConfigProperties.getB2c().getCreateInvoiceDir()).listFiles()).isNotEmpty();
    }

    @Test
    void voidEInvoice() {

        final ElectronicInvoice electronicInvoice = createMockElectronicInvoice();

        final PendingEInvoiceQueue pendingEInvoiceQueue = pendingEInvoiceQueueService.createPendingEInvoiceQueue(electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.VOID);

        final ElectronicInvoice updatedEInvoice = eInvoiceMessageService.createElectronicInvoiceMIG(pendingEInvoiceQueue);

        assertThat(updatedEInvoice.getInvoiceStatus()).isEqualByComparingTo(ElectronicInvoice.InvoiceStatus.MIG_CREATED);
        assertThat(new File(turnkeyConfigProperties.getB2c().getVoidInvoiceDir()).listFiles()).isNotEmpty();
    }

    private ElectronicInvoice createMockElectronicInvoice() {

        final ElectronicInvoice electronicInvoice = new ElectronicInvoice(ObjectId.get().toString(),
                "AG-12345678",
                new ElectronicInvoice.InvoicePeriod(ZoneId.of("Asia/Taipei")),
                new BigDecimal("150"),
                new BigDecimal("7.5"),
                "83515813",
                "Rain App",
                List.of(new ElectronicInvoice.InvoiceItem("coffee", 1, new BigDecimal("150"), new BigDecimal("150"))));

        return electronicInvoiceRepository.save(electronicInvoice);
    }

    @Test
    void createUnusedInvoiceNumber() {

        final String currentRangeIdentifier = invoiceNumberRangeService.getCurrentRangeIdentifier();
        InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange("83515813", currentRangeIdentifier, "AA", "10001001", "10001999");

        eInvoiceMessageService.createUnusedInvoiceNumberMIG(invoiceNumberRange);

        assertThat(invoiceNumberRange.getInvoiceIdentifier()).isNotNull();
        assertThat(new File(turnkeyConfigProperties.getB2p().getUnusedInvoiceNumberDir()).listFiles()).isNotEmpty();
    }
}