package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.einv.msg.EINVPayload;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class EInvoicePayloadUploader {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoicePayloadUploader.class);

    protected final String uploadDirectory;

    protected EInvoicePayloadUploader(String uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }


    public static EInvoicePayloadUploader select(PendingEInvoiceQueue pendingEInvoiceQueue, TurnkeyConfigProperties configProperties) {

        switch (pendingEInvoiceQueue.getInvoiceType()) {
            case CREATE:
                return new CreateEInvoicePayloadUploader(configProperties.getB2c().getCreateInvoiceDir());
            case VOID:
                return new VoidEInvoicePayloadUploader(configProperties.getB2c().getVoidInvoiceDir());
            default:
                throw new RuntimeException("Invoice type is not supported: " + pendingEInvoiceQueue.getInvoiceType());
        }
    }

    public final EINVPayload buildAndUpload(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue pendingEInvoiceQueue) {

        LOGGER.info("Building e-invoice payload {}", electronicInvoice.getInvoiceNumber());

        final EINVPayload einvPayload = this.constructEInvoicePayload(electronicInvoice, pendingEInvoiceQueue);
        EINVPayloadCopier.copyPayload(uploadDirectory, einvPayload);

        LOGGER.info("Building e-invoice payload {} done", electronicInvoice.getInvoiceNumber());

        return einvPayload;
    }

    protected abstract EINVPayload constructEInvoicePayload(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue pendingEInvoiceQueue);
}
