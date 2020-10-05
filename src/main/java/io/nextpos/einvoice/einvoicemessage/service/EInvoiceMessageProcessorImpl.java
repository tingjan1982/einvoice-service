package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class EInvoiceMessageProcessorImpl implements EInvoiceMessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageProcessorImpl.class);

    private final EInvoiceMessageService eInvoiceMessageService;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final JdbcTemplate jdbcTemplate;

    private final PlatformTransactionManager transactionManager;

    @Autowired
    public EInvoiceMessageProcessorImpl(EInvoiceMessageService eInvoiceMessageService, PendingEInvoiceQueueService pendingEInvoiceQueueService, ElectronicInvoiceRepository electronicInvoiceRepository, DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.eInvoiceMessageService = eInvoiceMessageService;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionManager = transactionManager;
    }

    @Override
    public void processEInvoiceMessages() {
        this.processEInvoiceMessages(() -> pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING));
    }

    @Override
    public void processEInvoiceMessages(Supplier<List<PendingEInvoiceQueue>> pendingEInvoicesProvider) {

        LOGGER.info("Start processing");
        final StopWatch mainTimer = new StopWatch("create einvoice messages");
        mainTimer.start("main");
        final List<PendingEInvoiceQueue> pendingEInvoices = pendingEInvoicesProvider.get();

        if (CollectionUtils.isNotEmpty(pendingEInvoices)) {
            LOGGER.info("Processing {} pending e-invoice(s)", pendingEInvoices.size());
            StopWatch timer = new StopWatch();

            pendingEInvoices.forEach(inv -> processEInvoice(timer, inv));
        } else {
            LOGGER.info("No pending e-invoice(s) to process.");
        }

        mainTimer.stop();
        LOGGER.info("{}", mainTimer.prettyPrint());
        LOGGER.info("End processing");
    }

    private void processEInvoice(StopWatch timer, PendingEInvoiceQueue pendingEInvoice) {

        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                timer.start(pendingEInvoice.getInvoiceNumber());
                LOGGER.info("Processing invoice id [type={}]: {}", pendingEInvoice.getInvoiceNumber(), pendingEInvoice.getInvoiceType());

                eInvoiceMessageService.createElectronicInvoiceMIG(pendingEInvoice);

                pendingEInvoice.markAsProcessed();
                pendingEInvoiceQueueService.updatePendingEInvoiceQueue(pendingEInvoice);

                LOGGER.info("Finished processing invoice id: {}", pendingEInvoice.getInvoiceNumber());
                timer.stop();
                LOGGER.info("{}", timer.prettyPrint());
            }
        });
    }

    /**
     * JdbcTemplate reference:
     *
     * https://www.baeldung.com/spring-jdbc-jdbctemplate
     */
    @Override
    public void updateEInvoiceStatus() {

        final List<PendingEInvoiceQueue> processedEInvoices = pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PROCESSED);

        if (CollectionUtils.isEmpty(processedEInvoices)) {
            return;
        }

        LOGGER.info("Found {} processed e-invoice(s), check and update status", processedEInvoices.size());
        AtomicInteger count = new AtomicInteger();

        processedEInvoices.forEach(inv -> {
            PreparedStatementSetter setter = new ArgumentPreparedStatementSetter(new Object[]{inv.getInvoiceIdentifier()});
            jdbcTemplate.query("select * from turnkey_message_log where invoice_identifier = ?",
                    setter,
                    rs -> {
                        count.incrementAndGet();
                        final String status = rs.getString("status");
                        final PendingEInvoiceQueue.PendingEInvoiceStatus invoiceStatus = resolveInvoiceStatus(status);

                        if (invoiceStatus != null) {
                            LOGGER.info("Updating processed invoice {} to status {}", inv.getInvoiceIdentifier(), invoiceStatus);

                            inv.setStatus(invoiceStatus);
                            pendingEInvoiceQueueService.updatePendingEInvoiceQueue(inv);

                            if (invoiceStatus == PendingEInvoiceQueue.PendingEInvoiceStatus.CONFIRMED) {
                                final ElectronicInvoice electronicInvoice = inv.getElectronicInvoice();
                                ElectronicInvoice.InvoiceStatus einvStatus = resolveElectronicInvoiceStatus(inv);

                                LOGGER.info("Updating electronic invoice {} status to {}", electronicInvoice.getId(), einvStatus);

                                electronicInvoice.setInvoiceStatus(einvStatus);
                                electronicInvoiceRepository.save(electronicInvoice);
                            }
                        }
                    });
        });

        LOGGER.info("Processed {} einvoice(s)", count.intValue());
    }

    private PendingEInvoiceQueue.PendingEInvoiceStatus resolveInvoiceStatus(String status) {

        switch (status) {
            case "G":
                return PendingEInvoiceQueue.PendingEInvoiceStatus.UPLOADED;
            case "C":
                return PendingEInvoiceQueue.PendingEInvoiceStatus.CONFIRMED;
            case "E":
                return PendingEInvoiceQueue.PendingEInvoiceStatus.ERROR;
            default:
                return null;
        }
    }

    private ElectronicInvoice.InvoiceStatus resolveElectronicInvoiceStatus(PendingEInvoiceQueue pendingEInvoiceQueue) {

        ElectronicInvoice.InvoiceStatus einvStatus = null;

        switch (pendingEInvoiceQueue.getInvoiceType()) {
            case CREATE:
                einvStatus = ElectronicInvoice.InvoiceStatus.PROCESSED;
                break;
            case VOID:
                einvStatus = ElectronicInvoice.InvoiceStatus.VOID;
                break;
        }
        return einvStatus;
    }

    @Override
    public void deleteProcessedQueues() {

        final StopWatch timer = new StopWatch("delete einvoice messages");
        timer.start("delete main");
        pendingEInvoiceQueueService.deleteByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.CONFIRMED);

        timer.stop();
        LOGGER.info("{}", timer.prettyPrint());
    }
}
