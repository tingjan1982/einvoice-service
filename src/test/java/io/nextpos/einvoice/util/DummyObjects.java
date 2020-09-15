package io.nextpos.einvoice.util;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

public class DummyObjects {

    public static ElectronicInvoice dummyElectronicInvoice() {
        return new ElectronicInvoice(ObjectId.get().toString(),
                "AG-12345678",
                new ElectronicInvoice.InvoicePeriod(ZoneId.of("Asia/Taipei")),
                new BigDecimal("150"),
                new BigDecimal("7.5"),
                "83515813",
                "Rain App",
                List.of(new ElectronicInvoice.InvoiceItem("coffee", 1, new BigDecimal("150"), new BigDecimal("150"))));
    }
}
