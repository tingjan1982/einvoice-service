package io.nextpos.einvoice.einvoicemessage.web.model;

import io.nextpos.einvoice.report.data.EInvoiceCheckReport;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EInvoiceCheckReportsResponse {

    private List<EInvoiceCheckReport> results;

    public boolean needAttention() {
        return results.stream()
                .anyMatch(r -> r.isContainsPendingEInvoice() || r.isContainsErrorEInvoice());
    }
}
