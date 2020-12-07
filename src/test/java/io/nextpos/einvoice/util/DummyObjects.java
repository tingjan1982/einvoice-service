package io.nextpos.einvoice.util;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

public class DummyObjects {

    public static ElectronicInvoice dummyElectronicInvoice(String invoiceNumber) {
        return dummyElectronicInvoice(invoiceNumber, "83515813");
    }

    public static ElectronicInvoice dummyElectronicInvoice(String invoiceNumber, String ubn) {
        return new ElectronicInvoice(
                "clientId",
                ObjectId.get().toString(),
                invoiceNumber,
                ElectronicInvoice.InvoiceStatus.CREATED,
                new ElectronicInvoice.InvoicePeriod(ZoneId.of("Asia/Taipei")),
                new BigDecimal("150"),
                new BigDecimal("7.5"),
                ubn,
                "Rain App",
                "Address",
                List.of(new ElectronicInvoice.InvoiceItem("coffee", 1, new BigDecimal("150"), new BigDecimal("150"))));
    }
}
