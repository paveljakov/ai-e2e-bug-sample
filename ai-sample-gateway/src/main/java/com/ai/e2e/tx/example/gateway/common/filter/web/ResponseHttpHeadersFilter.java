package com.ai.e2e.tx.example.gateway.common.filter.web;

import com.ai.e2e.tx.example.gateway.common.config.header.ReturnedHeadersConfig;
import com.ai.e2e.tx.example.gateway.common.util.HttpHeadersForwarder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class ResponseHttpHeadersFilter implements WebFilter {

  private final ReturnedHeadersConfig returnedHeadersConfig;

  public ResponseHttpHeadersFilter(final ReturnedHeadersConfig returnedHeadersConfig) {
    this.returnedHeadersConfig = returnedHeadersConfig;
  }

  @Override
  public Mono<Void> filter(final ServerWebExchange serverWebExchange, final WebFilterChain webFilterChain) {
    final HttpHeaders httpHeaders = serverWebExchange.getResponse().getHeaders();
    serverWebExchange.getResponse().beforeCommit(() -> {
      final Set<Map.Entry<String, List<String>>> actualHeaders = aggregateHttpHeadersAndClear(httpHeaders);
      final HttpHeadersForwarder httpHeadersForwarder = new HttpHeadersForwarder(returnedHeadersConfig, actualHeaders);
      httpHeadersForwarder.accept(httpHeaders);
      returnedHeadersConfig.getStaticReturnHeaders().forEach(httpHeaders::addIfAbsent);
      return Mono.empty();
    });
    return webFilterChain.filter(serverWebExchange);
  }

  private Set<Map.Entry<String, List<String>>> aggregateHttpHeadersAndClear(final HttpHeaders httpHeaders) {
    final Set<Map.Entry<String, List<String>>> currentHeaders = new HashSet<>();
    httpHeaders.forEach((key, value) -> currentHeaders.add(new HashMap.SimpleImmutableEntry<>(key, value)));
    httpHeaders.clear();
    return currentHeaders;
  }

}
