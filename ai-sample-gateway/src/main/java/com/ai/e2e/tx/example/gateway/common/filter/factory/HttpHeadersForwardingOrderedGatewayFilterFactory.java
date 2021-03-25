package com.ai.e2e.tx.example.gateway.common.filter.factory;

import com.ai.e2e.tx.example.gateway.common.config.header.ForwardedHeadersConfig;
import com.ai.e2e.tx.example.gateway.common.util.HttpHeadersForwarder;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Filter factory used to build a GatewayFilter that forwards HTTP headers in routes.
 */
public class HttpHeadersForwardingOrderedGatewayFilterFactory
    extends AbstractGatewayFilterFactory<ForwardedHeadersConfig> {

  public static final int ORDER_OF_FILTER = 0;

  @Override
  public OrderedGatewayFilter apply(final ForwardedHeadersConfig forwardedHeadersConfig) {
    return new OrderedGatewayFilter((exchange, chain) -> {
      final ServerHttpRequest.Builder serverHttpRequestBuilder = exchange.getRequest().mutate();

      final Set<Map.Entry<String, List<String>>> actualRequestHeaders =
          getActualRequestHeadersAndClearHttpHeaders(serverHttpRequestBuilder);
      serverHttpRequestBuilder.headers(new HttpHeadersForwarder(forwardedHeadersConfig, actualRequestHeaders));

      final ServerHttpRequest serverHttpRequest = serverHttpRequestBuilder.build();
      return chain.filter(exchange.mutate().request(serverHttpRequest).build());
    }, ORDER_OF_FILTER);
  }

  private Set<Map.Entry<String, List<String>>> getActualRequestHeadersAndClearHttpHeaders(
      final ServerHttpRequest.Builder serverHttpRequestBuilder) {
    final Set<Map.Entry<String, List<String>>> currentHeaders = new HashSet<>();
    serverHttpRequestBuilder.headers(httpHeaders -> {
      httpHeaders.forEach((key, value) -> currentHeaders.add(new HashMap.SimpleImmutableEntry<>(key, value)));
      httpHeaders.clear();
    });
    return currentHeaders;
  }

}
