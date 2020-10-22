package io.nextpos.einvoice.einvoicemessage.web;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.common.invoice.PendingInvoiceStats;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.einvoice.einvoicemessage.service.EInvoiceMessageProcessor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/einvoice")
public class EInvoiceMessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageController.class);

    private final EInvoiceMessageProcessor eInvoiceMessageProcessor;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    @Autowired
    public EInvoiceMessageController(EInvoiceMessageProcessor eInvoiceMessageProcessor, PendingEInvoiceQueueService pendingEInvoiceQueueService, InvoiceNumberRangeService invoiceNumberRangeService) {
        this.eInvoiceMessageProcessor = eInvoiceMessageProcessor;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
    }

    @GetMapping("/stats")
    public List<PendingInvoiceStats> stats() {
        return pendingEInvoiceQueueService.generatePendingEInvoiceStats();
    }

    @PostMapping("/process")
    public void processEInvoiceMessages(@RequestParam(value = "ubn", required = false) String ubn) {

        if (StringUtils.isNotBlank(ubn)) {
            LOGGER.info("Manually processing pending e-invoice queues for {}", ubn);
            eInvoiceMessageProcessor.processEInvoiceMessages(() -> pendingEInvoiceQueueService.findPendingEInvoicesByUbn(ubn));
        } else {
            eInvoiceMessageProcessor.processEInvoiceMessages();
        }
    }

    @PostMapping("/unusedInvoiceNumbers")
    public void processUnusedInvoiceNumbers(@Valid @RequestBody UnusedInvoiceNumberRequest request) {

        final InvoiceNumberRange invoiceNumberRange = invoiceNumberRangeService.getInvoiceNumberRangeByRangeIdentifier(request.getUbn(), request.getRangeIdentifier());
        eInvoiceMessageProcessor.processUnusedInvoiceNumber(invoiceNumberRange, request.getRangeFrom(), request.getRangeTo());
    }
}
