package io.nextpos.einvoice.einvoicemessage.service.processor;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.einvoicemessage.service.EInvoiceMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Service
public class PendingEInvoiceQueueProcessor extends AbstractEInvoiceObjectProcessor<PendingEInvoiceQueue> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingEInvoiceQueueProcessor.class);

    private final EInvoiceMessageService eInvoiceMessageService;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;


    public PendingEInvoiceQueueProcessor(EInvoiceMessageService eInvoiceMessageService, PendingEInvoiceQueueService pendingEInvoiceQueueService, PlatformTransactionManager transactionManager, ElectronicInvoiceRepository electronicInvoiceRepository, DataSource dataSource) {
        super(transactionManager, dataSource);

        this.eInvoiceMessageService = eInvoiceMessageService;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
    }

    @Override
    protected void processSingleObjectInternal(PendingEInvoiceQueue pendingEInvoice) {

        LOGGER.info("Processing e-invoice id [type={}]: {}", pendingEInvoice.getInvoiceNumber(), pendingEInvoice.getInvoiceType());

        eInvoiceMessageService.createElectronicInvoiceMIG(pendingEInvoice);
        pendingEInvoice.markAsProcessed();
        pendingEInvoiceQueueService.updatePendingEInvoiceQueue(pendingEInvoice);

        LOGGER.info("Finished processing e-invoice id: {}", pendingEInvoice.getInvoiceNumber());
    }

    @Override
    protected void updateObjectStatusInternal(PendingEInvoiceQueue pendingEInvoiceQueue, String turnkeyStatus) {

        final PendingEInvoiceQueue.PendingEInvoiceStatus invoiceStatus = resolveInvoiceStatus(turnkeyStatus);

        if (invoiceStatus != null) {
            LOGGER.info("Updating processed e-invoice {} to status {}", pendingEInvoiceQueue.getInvoiceNumber(), invoiceStatus);
            pendingEInvoiceQueue.setStatus(invoiceStatus);
            pendingEInvoiceQueueService.updatePendingEInvoiceQueue(pendingEInvoiceQueue);

            if (invoiceStatus == PendingEInvoiceQueue.PendingEInvoiceStatus.CONFIRMED) {
                final ElectronicInvoice electronicInvoice = pendingEInvoiceQueue.getElectronicInvoice();
                ElectronicInvoice.InvoiceStatus einvStatus = resolveElectronicInvoiceStatus(pendingEInvoiceQueue);

                LOGGER.info("Updating electronic invoice {} status to {}", electronicInvoice.getInvoiceNumber(), einvStatus);

                electronicInvoice.setInvoiceStatus(einvStatus);
                electronicInvoiceRepository.save(electronicInvoice);
            }
        }
    }

    private PendingEInvoiceQueue.PendingEInvoiceStatus resolveInvoiceStatus(String status) {

        switch (status) {
            case "G":
                return PendingEInvoiceQueue.PendingEInvoiceStatus.UPLOADED;
            case "C":
                return PendingEInvoiceQueue.PendingEInvoiceStatus.CONFIRMED;
            case "E":
                return PendingEInvoiceQueue.PendingEInvoiceStatus.ERROR;
            default:
                return null;
        }
    }

    private ElectronicInvoice.InvoiceStatus resolveElectronicInvoiceStatus(PendingEInvoiceQueue pendingEInvoiceQueue) {

        ElectronicInvoice.InvoiceStatus einvStatus = null;

        switch (pendingEInvoiceQueue.getInvoiceType()) {
            case CREATE:
                einvStatus = ElectronicInvoice.InvoiceStatus.PROCESSED;
                break;
            case VOID:
                einvStatus = ElectronicInvoice.InvoiceStatus.VOID;
                break;
        }
        return einvStatus;
    }
}
