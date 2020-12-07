package io.nextpos.einvoice.report.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TurnkeyMessageStats {

    private String ubn;

    private String status;

    private int count;
}
