package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final int REQUEST_LIMIT;
    private final TimeUnit TIME_UNIT;
    private int requestCount = 0;
    private final ReentrantLock LOCK = new ReentrantLock();
    private long lastRequestTime = System.currentTimeMillis();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.REQUEST_LIMIT = requestLimit;
        this.TIME_UNIT = timeUnit;
    }

    public void createDocument(Document document, String signature) {
        try {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastRequestTime;

            if (elapsedTime >= TIME_UNIT.toMillis(1)) {
                requestCount = 0;
            }

            if (requestCount < REQUEST_LIMIT) {
                requestCount++;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(TIME_UNIT.toMillis(1) - elapsedTime % TIME_UNIT.toMillis(1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            requestCount = 1;
            lastRequestTime = System.currentTimeMillis();

        } finally {
            LOCK.unlock();
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(document)))
                .build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(httpResponse.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildRequestBody(Document document) {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            String json = objectWriter.writeValueAsString(document);

            return json;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    private static class Document {
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    private static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        Document document = new Document();
        String signature = "example_signature";
        crptApi.createDocument(document, signature);
    }
}

