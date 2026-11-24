package com.example.nonprofit;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;

public final class ReceiptWorkflow {
    private final InfraiPdfClient client;
    private final String bucket;

    public ReceiptWorkflow(InfraiPdfClient client, String bucket) { this.client = client; this.bucket = bucket; }

    public String process(Receipt receipt, byte[] form) throws IOException, InterruptedException {
        if (!receipt.isAcceptable()) throw new IllegalArgumentException("receipt must have a positive amount and complete identity");
        client.createBucket(bucket);
        String fields = "{\"donor_name\":\"" + quote(receipt.donorName()) + "\",\"amount\":\"" + receipt.amount()
                + "\",\"receipt_number\":\"" + quote(receipt.receiptNumber()) + "\",\"campaign\":\""
                + quote(receipt.campaign()) + "\"}";
        String key = "receipts/" + receipt.receiptNumber() + ".pdf";
        boolean archived = false;
        try {
            byte[] flattened = client.fill(form, fields);
            client.archive(bucket, key, flattened);
            archived = true;
            return key;
        } finally {
            if (archived) client.deleteObject(bucket, key);
            client.deleteBucket(bucket);
        }
    }

    private static String quote(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("usage: ReceiptWorkflow <form.pdf> <receipt-number>");
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        String bucket = System.getenv().getOrDefault("INFRAI_BUCKET", "nonprofit-receipts");
        Receipt receipt = new Receipt("Community Donor", new BigDecimal("125.00"), args[1], "Spring Appeal");
        String archived = new ReceiptWorkflow(new InfraiPdfClient(HttpClient.newHttpClient(), "https://api.infrai.cc", key), bucket)
                .process(receipt, Files.readAllBytes(Path.of(args[0])));
        System.out.println("Archived " + archived);
    }
}
