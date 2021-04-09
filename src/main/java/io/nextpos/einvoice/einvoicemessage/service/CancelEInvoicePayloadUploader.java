package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.einv.msg.EINVPayload;
import com.tradevan.gateway.einv.msg.v32.C0501;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import org.apache.commons.lang3.StringUtils;

class CancelEInvoicePayloadUploader extends EInvoicePayloadUploader {

    public CancelEInvoicePayloadUploader(String uploadDirectory) {
        super(uploadDirectory);
    }

    @Override
    protected EINVPayload constructEInvoicePayload(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue pendingEInvoiceQueue) {

        final C0501 message = new C0501();
        message.setCancelInvoiceNumber(electronicInvoice.getInternalInvoiceNumber());
        message.setInvoiceDate(electronicInvoice.getInvoiceCreatedDate());
        message.setBuyerId(StringUtils.defaultIfBlank(electronicInvoice.getBuyerUbn(), "0000000000"));
        message.setSellerId(electronicInvoice.getSellerUbn());
        message.setCancelDate(pendingEInvoiceQueue.getCreatedDate());
        message.setCancelTime(pendingEInvoiceQueue.getCreatedDate());
        message.setCancelReason("Cancelled");

        return message;
    }
}
