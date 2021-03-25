package com.ai.e2e.tx.example.gateway.common.config;

import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.springframework.cloud.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;

@Primary
@Component
public class TcpClientPerRouteNettyRoutingFilter extends NettyRoutingFilter {

  public TcpClientPerRouteNettyRoutingFilter(
      final HttpClient httpClient,
      final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
      final HttpClientProperties properties
  ) {
    super(httpClient, headersFiltersProvider, properties);
  }

  @Override
  protected HttpClient getHttpClient(
      final Route route, final ServerWebExchange exchange
  ) {
    final HttpClient httpClient = super.getHttpClient(route, exchange);
    return httpClient.tcpConfiguration((tcpClient) -> configureTimeouts(route, tcpClient));
  }

  private TcpClient configureTimeouts(final Route route, final TcpClient tcpClient) {
    final Optional<Duration> responseTimeoutOptional = getResponseTimeout(route);
    return responseTimeoutOptional
        .map(duration -> tcpClient.doOnConnected(connection -> replaceDefaultReadTimeoutHandler(duration, connection)))
        .orElse(tcpClient);
  }

  private Connection replaceDefaultReadTimeoutHandler(final Duration duration, final Connection connection) {
    final ReadTimeoutHandler readTimeoutHandler = new ReadTimeoutHandler(duration.toMillis(), TimeUnit.MILLISECONDS);
    return connection
        .removeHandler(readTimeoutHandler.getClass().getSimpleName())
        .addHandlerLast(readTimeoutHandler);
  }

  private Optional<Duration> getResponseTimeout(final Route route) {
    final Object responseTimeoutAttr = route.getMetadata().get(RESPONSE_TIMEOUT_ATTR);
    Long responseTimeout = null;
    if (responseTimeoutAttr != null) {
      if (responseTimeoutAttr instanceof Number) {
        responseTimeout = ((Number) responseTimeoutAttr).longValue();
      } else {
        responseTimeout = Long.valueOf(responseTimeoutAttr.toString());
      }
    }
    return Optional.ofNullable(responseTimeout).map(Duration::ofMillis);
  }

}
