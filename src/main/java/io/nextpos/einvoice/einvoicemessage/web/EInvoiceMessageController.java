package io.nextpos.einvoice.einvoicemessage.web;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.common.invoice.PendingInvoiceStats;
import io.nextpos.einvoice.einvoicemessage.service.EInvoiceMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/einvoice")
public class EInvoiceMessageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageController.class);

    private final EInvoiceMessageProcessor eInvoiceMessageProcessor;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    @Autowired
    public EInvoiceMessageController(EInvoiceMessageProcessor eInvoiceMessageProcessor, PendingEInvoiceQueueService pendingEInvoiceQueueService) {
        this.eInvoiceMessageProcessor = eInvoiceMessageProcessor;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
    }

    @GetMapping("/stats")
    public PendingInvoiceStats stats() {
        return pendingEInvoiceQueueService.generatePendingEInvoiceStats();
    }

    @PostMapping("/process")
    public void manuallyProcess(@RequestParam("ubn") String ubn) {

        LOGGER.info("Manually processing pending einvoice queues for {}", ubn);
        eInvoiceMessageProcessor.processEInvoiceMessages(() -> pendingEInvoiceQueueService.findPendingEInvoicesByUbn(ubn));
    }
}
