package com.dan.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class RouteValidator {

    private static final List<String> openApiRegexEndpoints = List.of(
            "/identity/.*",
            "/files/preview/.*",
            "/files/upload",
            "/files/get/.*"
    );

    private static final List<String> secureApiRegexEndpoints = List.of(
    );

    private final List<Pattern> openApiPatterns = openApiRegexEndpoints.stream()
            .map(Pattern::compile)
            .collect(Collectors.toList());

    private final List<Pattern> secureApiPatterns = secureApiRegexEndpoints.stream()
            .map(Pattern::compile)
            .collect(Collectors.toList());

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();

        boolean isSecureApi = secureApiPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(path).matches());

        boolean isOpenApi = openApiPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(path).matches());

        return !(isOpenApi || !isSecureApi);
    };
}