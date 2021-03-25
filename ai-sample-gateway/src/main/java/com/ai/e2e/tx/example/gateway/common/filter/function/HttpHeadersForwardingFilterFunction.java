package com.ai.e2e.tx.example.gateway.common.filter.function;

import com.ai.e2e.tx.example.gateway.common.config.header.ForwardedHeadersConfig;
import com.ai.e2e.tx.example.gateway.common.util.HttpHeadersForwarder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter function that parses headers from context and puts them into downstream requests made via WebClient.
 */
@Component
public class HttpHeadersForwardingFilterFunction implements ExchangeFilterFunction {

  private final ForwardedHeadersConfig forwardedHeadersConfig;

  public HttpHeadersForwardingFilterFunction(final ForwardedHeadersConfig forwardedHeadersConfig) {
    this.forwardedHeadersConfig = forwardedHeadersConfig;
  }

  @Override
  public Mono<ClientResponse> filter(final ClientRequest clientRequest, final ExchangeFunction exchangeFunction) {
    return new HttpHeadersForwardingMono(clientRequest, exchangeFunction);
  }

  private class HttpHeadersForwardingMono extends Mono<ClientResponse> {

    private final ClientRequest clientRequest;

    private final ExchangeFunction exchangeFunction;

    private HttpHeadersForwardingMono(final ClientRequest clientRequest, final ExchangeFunction exchangeFunction) {
      super();
      this.clientRequest = clientRequest;
      this.exchangeFunction = exchangeFunction;
    }

    @Override
    public void subscribe(final CoreSubscriber<? super ClientResponse> coreSubscriber) {
      final Context currentContext = coreSubscriber.currentContext();
      final Set<Map.Entry<String, List<String>>> actualRequestHeaders = getActualRequestHeaders(currentContext);

      final ClientRequest.Builder requestBuilder = ClientRequest.from(clientRequest);
      requestBuilder.headers(new HttpHeadersForwarder(forwardedHeadersConfig, actualRequestHeaders));

      final ClientRequest requestWithForwardedHeaders = requestBuilder.build();
      exchangeFunction.exchange(requestWithForwardedHeaders).subscribe(coreSubscriber);
    }

    private Set<Map.Entry<String, List<String>>> getActualRequestHeaders(final Context context) {
      return context.stream()
          .filter((entry) -> entry.getKey() instanceof String && entry.getValue() instanceof List)
          .map((entry) -> new AbstractMap.SimpleEntry<String, List<String>>((String) entry.getKey(),
              context.getOrDefault(entry.getKey(), Collections.emptyList()) //avoiding unchecked cast
          ))
          .collect(Collectors.toSet());
    }

  }

}
