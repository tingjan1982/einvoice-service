package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;

import java.util.List;
import java.util.function.Supplier;

public interface EInvoiceMessageProcessor {

    void processEInvoiceMessages();

    void processEInvoiceMessages(Supplier<List<PendingEInvoiceQueue>> pendingEInvoicesProvider);

    void updateEInvoicesStatus();

    void processUnusedInvoiceNumbers();

    void processUnusedInvoiceNumber(InvoiceNumberRange invoiceNumberRange, String rangeFrom, String rangeTo);

    void updateInvoiceNumbersStatus();

    void updateInvoiceNumbersStatus(Supplier<List<InvoiceNumberRange>> invoiceNumberRangeProvider);

    void deleteProcessedQueues();
}
