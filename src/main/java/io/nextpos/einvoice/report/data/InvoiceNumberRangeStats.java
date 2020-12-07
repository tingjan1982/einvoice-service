package io.nextpos.einvoice.report.data;

import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class InvoiceNumberRangeStats {

    private String rangeIdentifier;

    private InvoiceNumberRange.InvoiceNumberRangeStatus status;

    private List<NumberRangeStats> numberRanges;

    public InvoiceNumberRangeStats(InvoiceNumberRange invoiceNumberRange) {

        rangeIdentifier = invoiceNumberRange.getRangeIdentifier();
        status = invoiceNumberRange.getStatus();
        numberRanges = invoiceNumberRange.getNumberRanges().stream()
                .map(NumberRangeStats::new)
                .collect(Collectors.toList());
    }

    @Data
    public static class NumberRangeStats {

        private String prefix;

        private String rangeFrom;

        private String rangeTo;

        private boolean finished;

        public NumberRangeStats(InvoiceNumberRange.NumberRange numberRange) {

            prefix = numberRange.getPrefix();
            rangeFrom = numberRange.getRangeFrom();
            rangeTo = numberRange.getRangeTo();
            finished = numberRange.isFinished();
        }
    }
}
