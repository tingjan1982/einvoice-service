package io.nextpos.einvoice.einvoicemessage.scheduler;

import io.nextpos.einvoice.einvoicemessage.service.EInvoiceMessageProcessor;
import io.nextpos.einvoice.shared.config.SchedulerConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduling reference:
 *
 * https://www.baeldung.com/spring-scheduled-tasks
 */
@Component
public class EInvoiceMessageScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EInvoiceMessageScheduler.class);

    private final EInvoiceMessageProcessor eInvoiceMessageProcessor;

    private final SchedulerConfigProperties schedulerConfigProperties;

    @Autowired
    public EInvoiceMessageScheduler(EInvoiceMessageProcessor eInvoiceMessageProcessor, SchedulerConfigProperties schedulerConfigProperties) {
        this.eInvoiceMessageProcessor = eInvoiceMessageProcessor;
        this.schedulerConfigProperties = schedulerConfigProperties;
    }

    /**
     * https://stackoverflow.com/questions/53194987/how-do-i-insert-an-emoji-in-a-java-string
     *
     * spEL:
     *
     * https://stackoverflow.com/questions/51818137/spring-boot-2-converting-duration-java-8-application-properties
     */
    @Scheduled(fixedRateString = "#{schedulerConfigProperties.heartbeatInterval.toMillis()}")
    public void heartbeat() {
        LOGGER.debug("heartbeat {}", "\u2764");
    }

    @Scheduled(fixedRateString = "#{schedulerConfigProperties.processMessageInterval.toMillis()}")
    public void createEInvoiceMessages() {
        eInvoiceMessageProcessor.processEInvoiceMessages();
    }

    @Scheduled(initialDelay = 20000, fixedRateString = "#{schedulerConfigProperties.updateInvoiceStatusInterval.toMillis()}")
    public void updateEInvoiceStatus() {
        eInvoiceMessageProcessor.updateEInvoiceStatus();
    }

    @Scheduled(cron = "0 0 5 ? * *")
    public void deleteProcessedQueues() {
        eInvoiceMessageProcessor.deleteProcessedQueues();
    }
}
