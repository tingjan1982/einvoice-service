package io.nextpos.einvoice.report.service;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueRepository;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.einvoice.report.data.EInvoiceCheckReport;
import io.nextpos.einvoice.util.DummyObjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EInvoiceCheckReportServiceImplTest {

    private final EInvoiceCheckReportService eInvoiceCheckReportService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    EInvoiceCheckReportServiceImplTest(EInvoiceCheckReportService eInvoiceCheckReportService, ElectronicInvoiceRepository electronicInvoiceRepository, PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository, InvoiceNumberRangeService invoiceNumberRangeService, JdbcTemplate jdbcTemplate) {
        this.eInvoiceCheckReportService = eInvoiceCheckReportService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.pendingEInvoiceQueueRepository = pendingEInvoiceQueueRepository;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void generateEInvoiceReport() {

        String ubn = "83515813";

        for (int i = 0; i < 5; i++) {
            String invoiceNumber = "AG-1000100" + i;
            final ElectronicInvoice einvoice = DummyObjects.dummyElectronicInvoice(invoiceNumber, ubn);
            electronicInvoiceRepository.save(einvoice);

            createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE, PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING);
            createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE, PendingEInvoiceQueue.PendingEInvoiceStatus.PROCESSED);
            createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE, PendingEInvoiceQueue.PendingEInvoiceStatus.CONFIRMED);
            createPendingEInvoiceQueue(einvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE, PendingEInvoiceQueue.PendingEInvoiceStatus.ERROR);
        }

        AtomicInteger seqNo = new AtomicInteger();
        pendingEInvoiceQueueRepository.findAll().forEach(q -> {
            final String id = String.valueOf(seqNo.incrementAndGet());
            jdbcTemplate.update("insert into turnkey_message_log (seqno, subseqno, from_party_id, status) values (?, ?, ?, ?)",
                    id, id, ubn, "C");
        });

        final InvoiceNumberRange invoiceNumberRange = new InvoiceNumberRange(ubn, invoiceNumberRangeService.getCurrentRangeIdentifier(), "AG", "10011001", "10011999");
        invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        final List<EInvoiceCheckReport> eInvoiceCheckReports = eInvoiceCheckReportService.generateEInvoiceCheckReport();

        assertThat(eInvoiceCheckReports).hasSize(1);
        assertThat(eInvoiceCheckReports.get(0)).satisfies(r -> {
            assertThat(r.getUbn()).isEqualTo(ubn);
            assertThat(r.getExpectedInvoiceTotal()).isEqualTo(20);
            assertThat(r.getActualInvoiceTotal()).isEqualTo(20);
            assertThat(r.getPendingInvoiceStats()).hasSize(4);
            assertThat(r.getTurnkeyMessageStats()).hasSize(1);
            assertThat(r.getActiveInvoiceNumberRanges()).hasSize(1);
            assertThat(r.isContainsPendingEInvoice()).isTrue();
            assertThat(r.isContainsErrorEInvoice()).isTrue();
        });
    }

    private void createPendingEInvoiceQueue(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType invoiceType, PendingEInvoiceQueue.PendingEInvoiceStatus status) {

        final PendingEInvoiceQueue pendingEInvoiceQueue = new PendingEInvoiceQueue(electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE);
        pendingEInvoiceQueue.setStatus(status);

        pendingEInvoiceQueueRepository.save(pendingEInvoiceQueue);
    }
}