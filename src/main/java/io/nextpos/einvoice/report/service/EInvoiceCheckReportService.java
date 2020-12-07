package io.nextpos.einvoice.report.service;

import io.nextpos.einvoice.report.data.EInvoiceCheckReport;

import java.util.List;

public interface EInvoiceCheckReportService {

    List<EInvoiceCheckReport> generateEInvoiceCheckReport();
}
