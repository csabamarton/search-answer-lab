package com.searchlab.embeddings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
public class EmbeddingClient {

    private final WebClient webClient;
    private final Duration timeout;

    public EmbeddingClient(@Value("${embeddings.base-url}") String baseUrl,
                          @Value("${embeddings.timeout-seconds:5}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public List<List<Double>> embed(List<String> texts) {
        try {
            EmbedRequest req = new EmbedRequest(texts);
            
            EmbedResponse response = webClient.post()
                    .uri("/embed")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(EmbedResponse.class)
                    .timeout(timeout)
                    .block();
            
            return response.vectors();
        } catch (RuntimeException e) {
            throw new EmbeddingServiceException("Failed to get embeddings: " + e.getMessage(), e);
        }
    }

    public record EmbedRequest(List<String> texts) {}
    public record EmbedResponse(String model, int dim, List<List<Double>> vectors) {}
    
    public static class EmbeddingServiceException extends RuntimeException {
        public EmbeddingServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
