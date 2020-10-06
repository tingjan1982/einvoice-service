package io.nextpos.einvoice.einvoicemessage.service.processor;

import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.einvoice.einvoicemessage.service.EInvoiceMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Service
public class InvoiceNumberRangeProcessor extends AbstractEInvoiceObjectProcessor<InvoiceNumberRange> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceNumberRangeProcessor.class);

    private final EInvoiceMessageService eInvoiceMessageService;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    @Autowired
    public InvoiceNumberRangeProcessor(PlatformTransactionManager transactionManager, DataSource dataSource, EInvoiceMessageService eInvoiceMessageService, InvoiceNumberRangeService invoiceNumberRangeService) {
        super(transactionManager, dataSource);

        this.eInvoiceMessageService = eInvoiceMessageService;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
    }

    @Override
    protected void processSingleObjectInternal(InvoiceNumberRange invoiceNumberRange) {

        LOGGER.info("Processing invoice number range {} {}", invoiceNumberRange.getUbn(), invoiceNumberRange.getRangeIdentifier());

        final InvoiceNumberRange updatedInvoiceNumberRange = eInvoiceMessageService.createUnusedInvoiceNumberMIG(invoiceNumberRange);
        invoiceNumberRangeService.saveInvoiceNumberRange(updatedInvoiceNumberRange);

        LOGGER.info("Finished invoice number range {} {}", updatedInvoiceNumberRange.getUbn(), updatedInvoiceNumberRange.getRangeIdentifier());
    }

    @Override
    protected void updateObjectStatusInternal(InvoiceNumberRange invoiceNumberRange, String turnkeyStatus) {

        final InvoiceNumberRange.InvoiceNumberRangeStatus status = resolveInvoiceNumberRangeStatus(turnkeyStatus);

        if (status != null) {
            LOGGER.info("Updating processed invoice number range {} {} to status {}", invoiceNumberRange.getUbn(), invoiceNumberRange.getRangeIdentifier(), status);

            invoiceNumberRange.setStatus(status);
            invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);
        }
    }

    private InvoiceNumberRange.InvoiceNumberRangeStatus resolveInvoiceNumberRangeStatus(String turnkeyStatus) {

        if ("C".equals(turnkeyStatus)) {
            return InvoiceNumberRange.InvoiceNumberRangeStatus.FINISHED;
        }

        return null;
    }
}
