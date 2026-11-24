package com.example.nonprofit;

import java.math.BigDecimal;

public final class ReceiptWorkflowTest {
    public static void main(String[] args) {
        Receipt invalid = new Receipt("Ada Donor", new BigDecimal("-1.00"), "r-001", "Winter Drive");
        if (invalid.isAcceptable()) throw new AssertionError("negative donations must be rejected");
        System.out.println("PASS: negative donation rejected before remote calls");
    }
}
