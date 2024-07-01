package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.*;

public class CrptApi {
    private ScheduledExecutorService executor;
    private final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10)).build();
    ;
    private HttpRequest httpRequest;
    private ObjectMapper objectMapper;
    private Semaphore semaphore;
    private final TimeUnit timeUnit;
    private int requestLimit = 0;


    public CrptApi(int requestLimit, TimeUnit timeUnit) {
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.objectMapper = new ObjectMapper();
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.timeUnit = timeUnit;
    }

    private void startSchedule() {
        long delay = timeUnit.toMillis(1);
        executor.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()), delay, delay, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        try {
            String jsonDocument = objectMapper.writeValueAsString(document);
            httpRequest = HttpRequest.newBuilder().uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());

        } finally {
            semaphore.release();
        }

    }

    public static class Document {
        public Description description;
        public String docId;
        public String docStatus;
        public String docType = "LP_INTRODUCE_GOODS";
        public boolean importRequest = true;
        public String ownerInn;
        public String participantInn;
        public String producerInn;
        public Date productionDate;
        public String productionType;
        public Products[] products;
        public Date regDate;
        public String regNumber;

        public static class Description {
            public String participantInn;
        }

        public static class Products {
            public String certificateDocument;
            public String certificateDocumentDate;
            public String certificateDocumentNumber;
            public String ownerInn;
            public String producerInn;
            public Date productionDate;
            public String tnvedCode;
            public String uitCode;
            public String uituCode;

        }

    }


}