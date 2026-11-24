package com.example.nonprofit;

import java.math.BigDecimal;

public record Receipt(String donorName, BigDecimal amount, String receiptNumber, String campaign) {
    public boolean isAcceptable() {
        return donorName != null && !donorName.isBlank() && amount != null && amount.signum() > 0
                && receiptNumber != null && !receiptNumber.isBlank() && campaign != null && !campaign.isBlank();
    }
}
