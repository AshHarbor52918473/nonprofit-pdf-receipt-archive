package com.example.nonprofit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class InfraiPdfClient {
    private final HttpClient http;
    private final String baseUrl;
    private final String apiKey;

    public InfraiPdfClient(HttpClient http, String baseUrl, String apiKey) {
        this.http = http;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public byte[] fill(byte[] pdf, String fieldsJson) throws IOException, InterruptedException {
        String body = "{\"pdf\":\"" + b64(pdf) + "\",\"fields\":" + fieldsJson + ",\"flatten\":true}";
        return post("/v1/pdf/form/fill", body);
    }

    public void createBucket(String bucket) throws IOException, InterruptedException {
        post("/v1/storage/bucket/create", "{\"name\":\"" + json(bucket) + "\"}");
    }

    public void archive(String bucket, String key, byte[] pdf) throws IOException, InterruptedException {
        String path = "/v1/storage/object/put/" + encode(bucket) + "/" + encode(key);
        request("PUT", path, "{\"data_base64\":\"" + b64(pdf) + "\"}");
    }

    public void deleteObject(String bucket, String key) throws IOException, InterruptedException {
        request("DELETE", "/v1/storage/object/delete/" + encode(bucket) + "/" + encode(key), null);
    }

    public void deleteBucket(String bucket) throws IOException, InterruptedException {
        request("DELETE", "/v1/storage/bucket/delete/" + encode(bucket), null);
    }

    private byte[] post(String path, String body) throws IOException, InterruptedException {
        return request("POST", path, body);
    }

    private byte[] request(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json");
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        HttpRequest request = builder.build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String envelope = response.body();
        if (!envelope.contains("\"ok\":true")) {
            throw new IOException("Infrai request rejected: " + envelope);
        }
        int data = envelope.indexOf("\"data\":\"");
        if (data >= 0) {
            int start = data + 9;
            int end = envelope.indexOf('"', start);
            return Base64.getDecoder().decode(envelope.substring(start, end));
        }
        return new byte[0];
    }

    private static String b64(byte[] value) { return Base64.getEncoder().encodeToString(value); }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String encode(String value) { return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
