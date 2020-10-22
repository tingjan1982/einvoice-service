package io.nextpos.einvoice.einvoicemessage.web;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class UnusedInvoiceNumberRequest {

    @NotBlank
    @Size(min = 8, max = 8)
    private String ubn;

    @NotBlank
    @Size(min = 6, max = 7)
    private String rangeIdentifier;

    @NotBlank
    @Size(min = 8, max = 8)
    private String rangeFrom;

    @NotBlank
    @Size(min = 8, max = 8)
    private String rangeTo;
}
