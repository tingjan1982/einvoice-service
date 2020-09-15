package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.function.Supplier;

@Component
public class EInvoiceMessageProcessorImpl implements EInvoiceMessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageProcessorImpl.class);

    private final EInvoiceMessageService eInvoiceMessageService;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository;

    private final MongoTransactionManager transactionManager;

    @Autowired
    public EInvoiceMessageProcessorImpl(EInvoiceMessageService eInvoiceMessageService, PendingEInvoiceQueueService pendingEInvoiceQueueService, PendingEInvoiceQueueRepository pendingEInvoiceQueueRepository, MongoTransactionManager transactionManager) {
        this.eInvoiceMessageService = eInvoiceMessageService;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.pendingEInvoiceQueueRepository = pendingEInvoiceQueueRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public void processEInvoiceMessages() {
        this.processEInvoiceMessages(() -> pendingEInvoiceQueueService.findPendingEInvoicesByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING));
    }

    @Override
    public void processEInvoiceMessages(Supplier<List<PendingEInvoiceQueue>> pendingEInvoicesProvider) {

        LOGGER.info("Start");
        final StopWatch mainTimer = new StopWatch("create einvoice messages");
        mainTimer.start("main");
        final List<PendingEInvoiceQueue> pendingEInvoices = pendingEInvoicesProvider.get();

        if (CollectionUtils.isNotEmpty(pendingEInvoices)) {
            LOGGER.info("Processing {} pending einvoice(s)", pendingEInvoices.size());
            StopWatch timer = new StopWatch();

            pendingEInvoices.forEach(inv -> processEInvoice(timer, inv));
        }

        mainTimer.stop();
        LOGGER.info("{}", mainTimer.prettyPrint());
        LOGGER.info("End");
    }

    private void processEInvoice(StopWatch timer, PendingEInvoiceQueue pendingEInvoice) {

        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                timer.start(pendingEInvoice.getInvoiceNumber());
                LOGGER.info("Processing invoice id: {}", pendingEInvoice.getInvoiceNumber());
                eInvoiceMessageService.createEInvoice(pendingEInvoice.getId());
                pendingEInvoice.markAsProcessed();
                pendingEInvoiceQueueRepository.save(pendingEInvoice);
                LOGGER.info("Finished processing invoice id: {}", pendingEInvoice.getInvoiceNumber());
                timer.stop();
                LOGGER.info("{}", timer.prettyPrint());
            }
        });
    }

    @Override
    public void deleteProcessedQueues() {

        final StopWatch timer = new StopWatch("delete einvoice messages");
        timer.start("delete main");
        pendingEInvoiceQueueRepository.deleteAllByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.PROCESSED);

        timer.stop();
        LOGGER.info("{}", timer.prettyPrint());
    }
}
