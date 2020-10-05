package io.nextpos.einvoice.einvoicemessage.service;

import com.tradevan.gateway.einv.msg.EINVPayload;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.shared.config.TurnkeyConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EInvoiceMessageServiceImpl implements EInvoiceMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageServiceImpl.class);

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final TurnkeyConfigProperties turnkeyConfigProperties;

    @Autowired
    public EInvoiceMessageServiceImpl(ElectronicInvoiceRepository electronicInvoiceRepository, TurnkeyConfigProperties turnkeyConfigProperties) {
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.turnkeyConfigProperties = turnkeyConfigProperties;
    }

    @Override
    public ElectronicInvoice createElectronicInvoiceMIG(PendingEInvoiceQueue pendingEInvoiceQueue) {

        final ElectronicInvoice electronicInvoice = pendingEInvoiceQueue.getElectronicInvoice();

        final EInvoicePayloadUploader uploader = EInvoicePayloadUploader.select(pendingEInvoiceQueue, turnkeyConfigProperties);
        final EINVPayload einvPayload = uploader.buildAndUpload(electronicInvoice, pendingEInvoiceQueue);
        
        electronicInvoice.setInvoiceStatus(ElectronicInvoice.InvoiceStatus.MIG_CREATED);

        final String invoiceIdentifier = einvPayload.getInvoiceIdentifier();
        LOGGER.info("Setting invoice identifier {} on pending e-invoice", invoiceIdentifier);
        pendingEInvoiceQueue.setInvoiceIdentifier(invoiceIdentifier);

        return electronicInvoiceRepository.save(electronicInvoice);
    }
}
