package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.einv.msg.EINVPayload;
import com.tradevan.gateway.einv.msg.v32.C0701;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import org.apache.commons.lang3.StringUtils;

public class VoidEInvoicePayloadUploader extends EInvoicePayloadUploader {

    public VoidEInvoicePayloadUploader(String voidInvoiceDir) {
        super(voidInvoiceDir);
    }

    @Override
    protected EINVPayload constructEInvoicePayload(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue pendingEInvoiceQueue) {

        final C0701 message = new C0701();
        message.setVoidInvoiceNumber(electronicInvoice.getInternalInvoiceNumber());
        message.setInvoiceDate(electronicInvoice.getInvoiceCreatedDate());
        message.setBuyerId(StringUtils.defaultIfBlank(electronicInvoice.getBuyerUbn(), "0000000000"));
        message.setSellerId(electronicInvoice.getSellerUbn());
        message.setVoidDate(pendingEInvoiceQueue.getCreatedDate());
        message.setVoidTime(pendingEInvoiceQueue.getCreatedDate());
        message.setVoidReason("Voided");

        return message;
    }
}
