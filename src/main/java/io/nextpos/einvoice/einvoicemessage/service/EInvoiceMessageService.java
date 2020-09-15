package io.nextpos.einvoice.einvoicemessage.service;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;

public interface EInvoiceMessageService {

    ElectronicInvoice createEInvoice(String id);
}
