package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.client.einv.parse.ParserHelper;
import com.tradevan.gateway.einv.msg.EINVPayload;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;

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

    public EINVPayload buildAndUpload(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue pendingEInvoiceQueue) {

        final EINVPayload einvPayload = this.constructEInvoicePayload(electronicInvoice, pendingEInvoiceQueue);
        this.sendPayloadToPath(electronicInvoice, einvPayload);

        return einvPayload;
    }

    protected abstract EINVPayload constructEInvoicePayload(ElectronicInvoice electronicInvoice, PendingEInvoiceQueue pendingEInvoiceQueue);

    private void sendPayloadToPath(ElectronicInvoice electronicInvoice, EINVPayload payload) {

        try {
            File migFile = new File(uploadDirectory, payload.getInvoiceIdentifier() + ".xml");

            LOGGER.info("Copying invoice {} to path {}", electronicInvoice.getInvoiceNumber(), migFile.getAbsolutePath());

            final ParserHelper parserHelper = new ParserHelper();
            final String einvoiceXML = parserHelper.marshalToXML(payload);
            LOGGER.debug("Invoice XML: {}", einvoiceXML);
            FileCopyUtils.copy(einvoiceXML, new FileWriter(migFile));

            LOGGER.info("Copying invoice {} done", electronicInvoice.getInvoiceNumber());

        } catch (Exception e) {
            LOGGER.error("Sending invoice {} error", electronicInvoice.getInvoiceNumber(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }


}
