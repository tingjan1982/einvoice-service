package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;

public interface EInvoiceMessageService {

    ElectronicInvoice createElectronicInvoiceMIG(PendingEInvoiceQueue pendingEInvoiceQueue);
}
