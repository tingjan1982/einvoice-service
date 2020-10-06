package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.client.einv.parse.ParserHelper;
import com.tradevan.gateway.einv.msg.EINVPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;

public class EINVPayloadCopier {

    private static final Logger LOGGER = LoggerFactory.getLogger(EINVPayloadCopier.class);

    public static void copyPayload(String uploadDirectory, EINVPayload payload) {

        final String invoiceIdentifier = payload.getInvoiceIdentifier();

        try {
            File migFile = new File(uploadDirectory, invoiceIdentifier + ".xml");

            LOGGER.info("Copying payload {} to path {}", invoiceIdentifier, migFile.getAbsolutePath());

            final ParserHelper parserHelper = new ParserHelper();
            final String payloadXML = parserHelper.marshalToXML(payload);
            LOGGER.debug("Payload XML: {}", payloadXML);
            FileCopyUtils.copy(payloadXML, new FileWriter(migFile));

            LOGGER.info("Copying payload {} done", invoiceIdentifier);

        } catch (Exception e) {
            LOGGER.error("Copying payload {} ERROR", invoiceIdentifier, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
