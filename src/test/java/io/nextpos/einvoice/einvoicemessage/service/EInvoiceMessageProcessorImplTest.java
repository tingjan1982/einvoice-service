package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.*;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import io.nextpos.einvoice.util.DummyObjects;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EInvoiceMessageProcessorImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageProcessorImplTest.class);

    private final EInvoiceMessageProcessor eInvoiceMessageProcessor;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository;

    private final TurnkeyConfigProperties turnkeyConfigProperties;

    private final JdbcTemplate jdbcTemplate;


    @Autowired
    EInvoiceMessageProcessorImplTest(EInvoiceMessageProcessor eInvoiceMessageProcessor, PendingEInvoiceQueueService pendingEInvoiceQueueService, ElectronicInvoiceRepository electronicInvoiceRepository, PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository, TurnkeyConfigProperties turnkeyConfigProperties, DataSource dataSource) {
        this.eInvoiceMessageProcessor = eInvoiceMessageProcessor;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.pendingEInvoiceQueueRepository = pendingEInvoiceQueueRepository;
        this.turnkeyConfigProperties = turnkeyConfigProperties;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void prepare() {
        createDirectory(turnkeyConfigProperties.getB2c().getCreateInvoiceDir());
        createDirectory(turnkeyConfigProperties.getB2c().getVoidInvoiceDir());

        for (int i = 0; i < 5; i++) {
            String invoiceNumber = "AG-1000100" + i;
            final ElectronicInvoice einvoice = DummyObjects.dummyElectronicInvoice(invoiceNumber);
            electronicInvoiceRepository.save(einvoice);
            pendingEInvoiceQueueService.createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE);
            pendingEInvoiceQueueService.createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.VOID);
        }
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
    }

    @Test
    void manageEInvoiceMessages() {
        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING)).hasSize(10);

        eInvoiceMessageProcessor.processEInvoiceMessages();

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING)).isEmpty();
        final List<PendingEInvoiceQueue> processedInvoices = pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PROCESSED);
        assertThat(processedInvoices).hasSize(10);
        assertThat(processedInvoices).allSatisfy(inv -> assertThat(inv.getInvoiceIdentifier()).isNotNull());

        AtomicInteger seqNo = new AtomicInteger();
        processedInvoices.forEach(inv -> {
            final String id = String.valueOf(seqNo.incrementAndGet());
            jdbcTemplate.update("insert into turnkey_message_log (seqno, subseqno, status, invoice_identifier) values (?, ?, ?, ?)",
                    id, id, "C", inv.getInvoiceIdentifier());
        });

        eInvoiceMessageProcessor.updateEInvoiceStatus();

        assertThat(electronicInvoiceRepository.findAll()).allSatisfy(inv -> assertThat(inv.getInvoiceStatus()).isIn(ElectronicInvoice.InvoiceStatus.PROCESSED, ElectronicInvoice.InvoiceStatus.VOID));

        eInvoiceMessageProcessor.deleteProcessedQueues();

        assertThat(pendingEInvoiceQueueRepository.findAll()).isEmpty();
    }

    @Test
    void generatePendingEInvoiceStats() {

        final PendingEInvoiceQueue pendingEInvoiceQueue = pendingEInvoiceQueueRepository.findAll().get(0);
        eInvoiceMessageProcessor.processEInvoiceMessages(() -> List.of(pendingEInvoiceQueue));

        final List<PendingInvoiceStats> stats = pendingEInvoiceQueueService.generatePendingEInvoiceStats();
        LOGGER.info("{}", stats);

        assertThat(stats).hasSize(2);
    }
}