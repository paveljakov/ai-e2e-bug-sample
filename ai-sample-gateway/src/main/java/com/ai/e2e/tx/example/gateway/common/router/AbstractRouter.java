package com.ai.e2e.tx.example.gateway.common.router;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.handler.predicate.QueryRoutePredicateFactory;
import org.springframework.web.server.ServerWebExchange;

import java.util.function.Predicate;

public abstract class AbstractRouter {

  @Autowired
  private GatewayFilter defaultRetryGatewayFilter;

  @Autowired
  private GatewayFilter defaultHeadersForwardingGatewayFilter;

  @Autowired
  private GatewayFilter defaultHystrixGatewayFilter;

  @Autowired
  private GatewayFilter defaultBasicAuthenticationGatewayFilter;

  protected abstract String prefix();

  protected abstract String getBaseUrl();

  protected Predicate<ServerWebExchange> queryParam(final String param) {
    return new QueryRoutePredicateFactory().apply(new QueryRoutePredicateFactory.Config().setParam(param));
  }

  protected GatewayFilter defaultRetryFilter() {
    return defaultRetryGatewayFilter;
  }

  protected GatewayFilter defaultHeadersForwardingFilter() {
    return defaultHeadersForwardingGatewayFilter;
  }

  protected GatewayFilter defaultHystrixForwardingFilter() {
    return defaultHystrixGatewayFilter;
  }

  protected GatewayFilter defaultBasicAuthFilter() {
    return defaultBasicAuthenticationGatewayFilter;
  }

}
