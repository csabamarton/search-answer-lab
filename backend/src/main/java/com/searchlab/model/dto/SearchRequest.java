package com.searchlab.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for incoming search requests.
 * Contains query parameters and search configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    private String mode; // "traditional" or "ai", defaults to "traditional"

    @Min(0)
    private Integer page = 0;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    private String traceId;
}
