package io.nextpos.einvoice.einvoicemessage.service.processor;

import io.nextpos.einvoice.common.shared.EInvoiceBaseObject;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

abstract class AbstractEInvoiceObjectProcessor<T extends EInvoiceBaseObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEInvoiceObjectProcessor.class);

    protected final PlatformTransactionManager transactionManager;

    protected final JdbcTemplate jdbcTemplate;

    protected AbstractEInvoiceObjectProcessor(PlatformTransactionManager transactionManager, DataSource dataSource) {
        this.transactionManager = transactionManager;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void processObjects(Supplier<List<T>> provider) {

        LOGGER.info(">> Start {}", this.getClass().getSimpleName());

        final List<T> objects = provider.get();

        if (CollectionUtils.isNotEmpty(objects)) {
            LOGGER.info("Processing {} pending message(s)", objects.size());
            StopWatch timer = new StopWatch();

            objects.forEach(o -> processSingleObject(o, timer));

            LOGGER.info("{}", timer.prettyPrint());
        } else {
            LOGGER.info("No pending message(s) to process.");
        }

        LOGGER.info(">> End {}", this.getClass().getSimpleName());
    }

    private void processSingleObject(T obj, StopWatch timer) {

        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                timer.start(Objects.requireNonNull(obj.getId()));

                processSingleObjectInternal(obj);

                timer.stop();
            }
        });
    }

    protected abstract void processSingleObjectInternal(T obj);

    /**
     * JdbcTemplate reference:
     *
     * https://www.baeldung.com/spring-jdbc-jdbctemplate
     */
    public void updateObjectsStatus(List<T> objects) {

        if (CollectionUtils.isEmpty(objects)) {
            return;
        }

        LOGGER.info("Found {} processed message(s), check and update status", objects.size());
        AtomicInteger count = new AtomicInteger();

        objects.forEach(o -> {
            PreparedStatementSetter setter = new ArgumentPreparedStatementSetter(new Object[]{o.getInvoiceIdentifier()});
            jdbcTemplate.query("select * from turnkey_message_log where invoice_identifier = ?",
                    setter,
                    rs -> {
                        count.incrementAndGet();
                        final String turnkeyStatus = rs.getString("status");
                        updateObjectStatusInternal(o, turnkeyStatus);
                    });
        });

        LOGGER.info("Processed {} message(s)", count.intValue());
    }

    protected abstract void updateObjectStatusInternal(T object, String turnkeyStatus);
}
