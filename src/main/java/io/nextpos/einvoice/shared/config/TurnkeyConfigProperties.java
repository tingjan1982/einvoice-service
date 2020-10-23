package io.nextpos.einvoice.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "turnkey")
@Data
public class TurnkeyConfigProperties {

    private String workingDir;

    private B2C b2c;

    private B2P b2p;

    @Data
    public static class B2C {

        private String createInvoiceDir;

        private String cancelInvoiceDir;

        private String voidInvoiceDir;
    }

    @Data
    public static class B2P {

        private String unusedInvoiceNumberDir;
    }
}
