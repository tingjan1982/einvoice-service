package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.*;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
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

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final TurnkeyConfigProperties turnkeyConfigProperties;

    private final JdbcTemplate jdbcTemplate;


    @Autowired
    EInvoiceMessageProcessorImplTest(EInvoiceMessageProcessor eInvoiceMessageProcessor, PendingEInvoiceQueueService pendingEInvoiceQueueService, ElectronicInvoiceRepository electronicInvoiceRepository, PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository, InvoiceNumberRangeService invoiceNumberRangeService, TurnkeyConfigProperties turnkeyConfigProperties, DataSource dataSource) {
        this.eInvoiceMessageProcessor = eInvoiceMessageProcessor;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.pendingEInvoiceQueueRepository = pendingEInvoiceQueueRepository;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.turnkeyConfigProperties = turnkeyConfigProperties;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @BeforeEach
    void prepare() {
        createDirectory(turnkeyConfigProperties.getB2c().getCreateInvoiceDir());
        createDirectory(turnkeyConfigProperties.getB2c().getCancelInvoiceDir());
        createDirectory(turnkeyConfigProperties.getB2c().getVoidInvoiceDir());
        createDirectory(turnkeyConfigProperties.getB2p().getUnusedInvoiceNumberDir());

        for (int i = 0; i < 5; i++) {
            String invoiceNumber = "AG-1000100" + i;
            final ElectronicInvoice einvoice = DummyObjects.dummyElectronicInvoice(invoiceNumber);
            electronicInvoiceRepository.save(einvoice);
            pendingEInvoiceQueueService.createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE);
            pendingEInvoiceQueueService.createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.CANCEL);
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
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2c().getCancelInvoiceDir()));
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2c().getVoidInvoiceDir()));
        FileUtils.deleteDirectory(new File(turnkeyConfigProperties.getB2p().getUnusedInvoiceNumberDir()));

        // todo: enable mongodb transaction so rollback occurs automatically.
        electronicInvoiceRepository.deleteAll();
        pendingEInvoiceQueueRepository.deleteAll();
    }

    @Test
    void processEInvoiceMessages() {

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByStatuses(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING)).hasSize(15);

        eInvoiceMessageProcessor.processEInvoiceMessages();

        assertThat(pendingEInvoiceQueueService.findPendingEInvoicesByStatuses(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING)).isEmpty();
        final List<PendingEInvoiceQueue> processedInvoices = pendingEInvoiceQueueService.findPendingEInvoicesByStatuses(PendingEInvoiceQueue.PendingEInvoiceStatus.PROCESSED);
        assertThat(processedInvoices).hasSize(15);
        assertThat(processedInvoices).allSatisfy(inv -> assertThat(inv.getInvoiceIdentifier()).isNotNull());

        AtomicInteger seqNo = new AtomicInteger();
        processedInvoices.forEach(inv -> {
            final String id = String.valueOf(seqNo.incrementAndGet());
            jdbcTemplate.update("insert into turnkey_message_log (seqno, subseqno, status, invoice_identifier) values (?, ?, ?, ?)",
                    id, id, "C", inv.getInvoiceIdentifier());
        });

        eInvoiceMessageProcessor.updateEInvoicesStatus();

        assertThat(electronicInvoiceRepository.findAll()).allSatisfy(inv -> assertThat(inv.getInvoiceStatus()).isIn(ElectronicInvoice.InvoiceStatus.PROCESSED, ElectronicInvoice.InvoiceStatus.CANCELLED, ElectronicInvoice.InvoiceStatus.VOID));

        eInvoiceMessageProcessor.deleteProcessedQueues();

        assertThat(pendingEInvoiceQueueRepository.findAll()).isEmpty();
    }

    @Test
    void processUnusedInvoiceNumbers() {

        int monthToSubtract = YearMonth.now().getMonthValue() % 2 == 0 ? 2 : 1;
        String rangeIdentifier = invoiceNumberRangeService.getRangeIdentifier(YearMonth.now().minusMonths(monthToSubtract));

        final InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange("83515813", rangeIdentifier, "AA", "10001001", "100001999");
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        eInvoiceMessageProcessor.processUnusedInvoiceNumbers();

        final List<InvoiceNumberRange> invoiceNumberRanges = invoiceNumberRangeService.getInvoiceNumberRangesByLastRangeIdentifier();
        AtomicInteger seqNo = new AtomicInteger();
        invoiceNumberRanges.forEach(inv -> {
            final String id = String.valueOf(seqNo.incrementAndGet());
            jdbcTemplate.update("insert into turnkey_message_log (seqno, subseqno, status, invoice_identifier) values (?, ?, ?, ?)",
                    id, id, "C", inv.getInvoiceIdentifier());
        });

        eInvoiceMessageProcessor.updateInvoiceNumbersStatus();

        assertThat(invoiceNumberRangeService.getInvoiceNumberRange(invoiceNumberRange.getId())).satisfies(range -> {
            assertThat(range.getInvoiceIdentifier()).isNotNull();
            assertThat(range.getStatus()).isEqualByComparingTo(InvoiceNumberRange.InvoiceNumberRangeStatus.FINISHED);
        });
    }

    @Test
    void processOneInvoiceNumberRange() {

        String rangeIdentifier = invoiceNumberRangeService.getRangeIdentifier(YearMonth.now());
        final String rangeFrom = "10001001";
        final String rangeTo = "100001999";
        final InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange("83515813", rangeIdentifier, "AA", rangeFrom, rangeTo);
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        eInvoiceMessageProcessor.processUnusedInvoiceNumber(invoiceNumberRange, rangeFrom, rangeTo);

        String id = "1";
        jdbcTemplate.update("insert into turnkey_message_log (seqno, subseqno, status, invoice_identifier) values (?, ?, ?, ?)",
                id, id, "C", invoiceNumberRange.getInvoiceIdentifier());

        eInvoiceMessageProcessor.updateInvoiceNumbersStatus(() -> List.of(invoiceNumberRange));

        assertThat(invoiceNumberRangeService.getInvoiceNumberRange(invoiceNumberRange.getId())).satisfies(range -> {
            assertThat(range.getInvoiceIdentifier()).isNotNull();
            assertThat(range.getStatus()).isEqualByComparingTo(InvoiceNumberRange.InvoiceNumberRangeStatus.FINISHED);
        });
    }

    @Test
    void generatePendingEInvoiceStats() {

        final PendingEInvoiceQueue pendingEInvoiceQueue = pendingEInvoiceQueueRepository.findAll().get(0);
        eInvoiceMessageProcessor.processEInvoiceMessages(() -> List.of(pendingEInvoiceQueue));

        final Map<String, List<PendingInvoiceStats>> stats = pendingEInvoiceQueueService.generatePendingEInvoiceStats();
        LOGGER.info("{}", stats);

        assertThat(stats.get("83515813")).hasSize(2);
    }
}