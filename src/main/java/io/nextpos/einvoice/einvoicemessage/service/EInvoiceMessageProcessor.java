package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;

import java.util.List;
import java.util.function.Supplier;

public interface EInvoiceMessageProcessor {

    void processEInvoiceMessages();

    void processEInvoiceMessages(Supplier<List<PendingEInvoiceQueue>> pendingEInvoicesProvider);

    void updateEInvoiceStatus();

    void deleteProcessedQueues();
}
