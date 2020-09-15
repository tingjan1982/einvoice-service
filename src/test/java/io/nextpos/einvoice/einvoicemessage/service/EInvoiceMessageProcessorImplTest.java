package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.*;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import io.nextpos.einvoice.util.DummyObjects;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EInvoiceMessageProcessorImplTest {

    private final EInvoiceMessageProcessor eInvoiceMessageProcessor;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository;

    private final TurnkeyConfigProperties turnkeyConfigProperties;

    @Autowired
    EInvoiceMessageProcessorImplTest(EInvoiceMessageProcessor eInvoiceMessageProcessor, PendingEInvoiceQueueService pendingEInvoiceQueueService, ElectronicInvoiceRepository electronicInvoiceRepository, PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository, TurnkeyConfigProperties turnkeyConfigProperties) {
        this.eInvoiceMessageProcessor = eInvoiceMessageProcessor;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.pendingEInvoiceQueueRepository = pendingEInvoiceQueueRepository;
        this.turnkeyConfigProperties = turnkeyConfigProperties;
    }

    @BeforeEach
    void prepare() {
        final File file = new File(turnkeyConfigProperties.getB2c().getCreateInvoiceDir());

        if (!file.exists()) {
            file.mkdirs();
        }

        for (int i = 0; i < 5; i++) {
            final ElectronicInvoice einvoice = DummyObjects.dummyElectronicInvoice();
            electronicInvoiceRepository.save(einvoice);
            pendingEInvoiceQueueService.savePendingEInvoiceQueue(einvoice);
        }
    }

    @AfterEach
    void teardown() throws Exception {
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2c().getCreateInvoiceDir()));
    }

    @Test
    void manageEInvoiceMessages() {
        eInvoiceMessageProcessor.processEInvoiceMessages();

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING)).isEmpty();
        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PROCESSED)).hasSize(5);

        eInvoiceMessageProcessor.deleteProcessedQueues();

        assertThat(pendingEInvoiceQueueRepository.findAll()).isEmpty();
    }

    @Test
    void test() {

        pendingEInvoiceQueueService.generatePendingEInvoiceStats();
    }
}