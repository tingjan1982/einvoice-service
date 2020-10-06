package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.einvoice.einvoicemessage.service.processor.InvoiceNumberRangeProcessor;
import io.nextpos.einvoice.einvoicemessage.service.processor.PendingEInvoiceQueueProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.function.Supplier;

@Component
public class EInvoiceMessageProcessorImpl implements EInvoiceMessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageProcessorImpl.class);

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final PendingEInvoiceQueueProcessor pendingEInvoiceQueueProcessor;

    private final InvoiceNumberRangeProcessor invoiceNumberRangeProcessor;

    @Autowired
    public EInvoiceMessageProcessorImpl(PendingEInvoiceQueueService pendingEInvoiceQueueService, InvoiceNumberRangeService invoiceNumberRangeService, PendingEInvoiceQueueProcessor pendingEInvoiceQueueProcessor, InvoiceNumberRangeProcessor invoiceNumberRangeProcessor) {
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.pendingEInvoiceQueueProcessor = pendingEInvoiceQueueProcessor;
        this.invoiceNumberRangeProcessor = invoiceNumberRangeProcessor;
    }

    @Override
    public void processEInvoiceMessages() {
        this.processEInvoiceMessages(() -> pendingEInvoiceQueueService.findPendingEInvoicesByStatuses(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING));
    }

    @Override
    public void processEInvoiceMessages(Supplier<List<PendingEInvoiceQueue>> pendingEInvoicesProvider) {

        pendingEInvoiceQueueProcessor.processObjects(pendingEInvoicesProvider);
    }

    @Override
    public void updateEInvoicesStatus() {

        final List<PendingEInvoiceQueue> processedEInvoices = pendingEInvoiceQueueService.findPendingEInvoicesByStatuses(PendingEInvoiceQueue.PendingEInvoiceStatus.PROCESSED, PendingEInvoiceQueue.PendingEInvoiceStatus.UPLOADED);

        pendingEInvoiceQueueProcessor.updateObjectsStatus(processedEInvoices);
    }

    @Override
    public void processUnusedInvoiceNumbers() {

        final List<InvoiceNumberRange> invoiceNumberRanges = invoiceNumberRangeService.getInvoiceNumberRangesByLastRangeIdentifier();

        invoiceNumberRangeProcessor.processObjects(() -> invoiceNumberRanges);
    }

    @Override
    public void updateInvoiceNumbersStatus() {

        final List<InvoiceNumberRange> invoiceNumberRanges = invoiceNumberRangeService.getInvoiceNumberRangesByLastRangeIdentifier();

        invoiceNumberRangeProcessor.updateObjectsStatus(invoiceNumberRanges);
    }

    @Override
    public void deleteProcessedQueues() {

        final StopWatch timer = new StopWatch("delete e-invoice messages");
        timer.start("delete main");
        pendingEInvoiceQueueService.deleteByStatus(PendingEInvoiceQueue.PendingEInvoiceStatus.CONFIRMED);

        timer.stop();
        LOGGER.info("{}", timer.prettyPrint());
    }
}
