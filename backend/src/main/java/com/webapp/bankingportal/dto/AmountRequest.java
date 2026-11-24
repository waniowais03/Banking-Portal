
package com.webapp.bankingportal.dto;

import java.math.BigDecimal;

public record AmountRequest(
    String accountNumber,
    String pin,
    BigDecimal amount
) {
}
