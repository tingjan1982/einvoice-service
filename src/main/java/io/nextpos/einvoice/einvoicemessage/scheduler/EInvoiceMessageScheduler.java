package io.nextpos.einvoice.einvoicemessage.scheduler;

import io.nextpos.einvoice.einvoicemessage.service.EInvoiceMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EInvoiceMessageScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageScheduler.class);

    private final EInvoiceMessageProcessor eInvoiceMessageProcessor;

    @Autowired
    public EInvoiceMessageScheduler(EInvoiceMessageProcessor eInvoiceMessageProcessor) {
        this.eInvoiceMessageProcessor = eInvoiceMessageProcessor;
    }

    /**
     * https://stackoverflow.com/questions/53194987/how-do-i-insert-an-emoji-in-a-java-string
     */
    @Scheduled(fixedRate = 60000)
    public void heartbeat() {
        LOGGER.info("{} heartbeat {}", this.getClass().getSimpleName(), "\u2764");
    }

    @Scheduled(initialDelay = 20000, fixedRate = 60 * 60 * 1000)
    public void createEInvoiceMessages() {
        eInvoiceMessageProcessor.processEInvoiceMessages();
    }

    @Scheduled(cron = "0 0 5 ? * *")
    public void deleteProcessedQueues() {
        eInvoiceMessageProcessor.deleteProcessedQueues();
    }
}
