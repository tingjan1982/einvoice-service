package io.nextpos.einvoice.report.data;

import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingInvoiceStats;
import lombok.Data;

import java.util.List;

@Data
public class EInvoiceCheckReport {

    private String ubn;

    private int expectedInvoiceTotal;

    private int actualInvoiceTotal;

    private List<PendingInvoiceStats> pendingInvoiceStats;

    private List<TurnkeyMessageStats> turnkeyMessageStats;

    private List<InvoiceNumberRangeStats> activeInvoiceNumberRanges;

    private boolean containsPendingEInvoice;

    private boolean containsErrorEInvoice;

    public EInvoiceCheckReport(String ubn, List<PendingInvoiceStats> pendingInvoiceStats, List<TurnkeyMessageStats> turnkeyMessageStats, List<InvoiceNumberRangeStats> invoiceNumberRanges) {
        this.ubn = ubn;
        this.pendingInvoiceStats = pendingInvoiceStats;
        this.turnkeyMessageStats = turnkeyMessageStats;
        this.activeInvoiceNumberRanges = invoiceNumberRanges;

        expectedInvoiceTotal = pendingInvoiceStats.stream()
                .mapToInt(PendingInvoiceStats::getInvoiceCount)
                .sum();

        actualInvoiceTotal = turnkeyMessageStats.stream()
                .mapToInt(TurnkeyMessageStats::getCount)
                .sum();

        containsPendingEInvoice = pendingInvoiceStats.stream()
                .anyMatch(s -> s.getStatus().equals(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING.name()));

        containsErrorEInvoice = pendingInvoiceStats.stream()
                .anyMatch(s -> s.getStatus().equals(PendingEInvoiceQueue.PendingEInvoiceStatus.ERROR.name()));
    }

}
