package com.l2d.tuto.springbootwebfluxsecurity.security.jwt;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author duc-d
 */
public class JWTHeadersExchangeMatcher implements ServerWebExchangeMatcher {
    @Override
    public Mono<MatchResult> matches(final ServerWebExchange exchange) {
        Mono<ServerHttpRequest> request = Mono.just(exchange).map(ServerWebExchange::getRequest);

        /* Check for header "Authorization" */
        return request.map(ServerHttpRequest::getHeaders)
                .filter(httpHeaders -> httpHeaders.containsKey(HttpHeaders.AUTHORIZATION))
                .flatMap(httpHeaders -> MatchResult.match())
                .switchIfEmpty(MatchResult.notMatch());
    }
}
