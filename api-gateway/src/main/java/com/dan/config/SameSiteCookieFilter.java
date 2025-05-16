package com.dan.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SameSiteCookieFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Duyệt qua tất cả các header
            exchange.getResponse().getHeaders().entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase("Set-Cookie")) // Chỉ xử lý Set-Cookie
                    .forEach(entry -> {
                        List<String> updatedCookies = entry.getValue().stream()
                                .map(cookie -> {
                                    if (!cookie.toLowerCase().contains("samesite")) {
                                        return cookie + "; SameSite=None; Secure";
                                    }
                                    return cookie;
                                })
                                .collect(Collectors.toList());
                        // Cập nhật lại header
                        exchange.getResponse().getHeaders().put("Set-Cookie", updatedCookies);
                    });
        }));
    }
}