package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;

public interface EInvoiceMessageService {

    ElectronicInvoice createElectronicInvoiceMIG(PendingEInvoiceQueue pendingEInvoiceQueue);

    InvoiceNumberRange createUnusedInvoiceNumberMIG(InvoiceNumberRange invoiceNumberRange);
}
