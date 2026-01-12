package com.searchlab.semantic;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class VectorSql {
    private VectorSql() {}

    /**
     * Converts a list of doubles to pgvector literal format: '[0.1,0.2,...]'
     */
    public static String toVectorLiteral(List<Double> vector) {
        String body = vector.stream()
                .map(d -> String.format(Locale.US, "%.8f", d))
                .collect(Collectors.joining(","));
        return "[" + body + "]";
    }
}
