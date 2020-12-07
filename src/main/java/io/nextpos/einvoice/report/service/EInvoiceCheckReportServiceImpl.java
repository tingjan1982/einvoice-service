package io.nextpos.einvoice.report.service;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.common.invoice.PendingInvoiceStats;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.einvoice.report.data.EInvoiceCheckReport;
import io.nextpos.einvoice.report.data.InvoiceNumberRangeStats;
import io.nextpos.einvoice.report.data.TurnkeyMessageStats;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
@Transactional
public class EInvoiceCheckReportServiceImpl implements EInvoiceCheckReportService {

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final JdbcTemplate jdbcTemplate;

    public EInvoiceCheckReportServiceImpl(PendingEInvoiceQueueService pendingEInvoiceQueueService, InvoiceNumberRangeService invoiceNumberRangeService, DataSource dataSource) {
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<EInvoiceCheckReport> generateEInvoiceCheckReport() {

        Map<String, List<PendingInvoiceStats>> pendingInvoiceStats = pendingEInvoiceQueueService.generatePendingEInvoiceStats();
        Map<String, List<InvoiceNumberRangeStats>> invoiceNumberRangesMap = invoiceNumberRangeService.getRecentInvoiceNumberRanges().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .map(InvoiceNumberRangeStats::new)
                        .collect(Collectors.toList())));

        Map<String, List<TurnkeyMessageStats>> turnkeyMessageStats = pendingInvoiceStats.keySet().stream()
                .map(ubn -> jdbcTemplate.query("select from_party_id ubn, status, count(*) count from turnkey_message_log where from_party_id = ? group by from_party_id, status",
                        (rs, rowNum) -> {
                                final String ubn1 = rs.getString("ubn");
                                final String status = rs.getString("status");
                                final int count = rs.getInt("count");
                                return new TurnkeyMessageStats(ubn1, status, count);
                        },
                        ubn))
                .flatMap(Collection::stream)
                .collect(groupingBy(TurnkeyMessageStats::getUbn));

        return pendingInvoiceStats.keySet().stream()
                .map(ubn -> {
                    final List<PendingInvoiceStats> expectedStats = pendingInvoiceStats.getOrDefault(ubn, List.of());
                    final List<InvoiceNumberRangeStats> invoiceNumberRanges = invoiceNumberRangesMap.getOrDefault(ubn, List.of());
                    final List<TurnkeyMessageStats> actualStats = turnkeyMessageStats.getOrDefault(ubn, List.of());

                    return new EInvoiceCheckReport(ubn, expectedStats, actualStats, invoiceNumberRanges);
                })
                .collect(Collectors.toList());
    }
}
