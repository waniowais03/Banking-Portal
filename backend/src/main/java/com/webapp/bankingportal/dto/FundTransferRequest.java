
package com.webapp.bankingportal.dto;

import java.math.BigDecimal;

public record FundTransferRequest(
    String sourceAccountNumber,
    String targetAccountNumber,
    BigDecimal amount,
    String pin
) {
}
