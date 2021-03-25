package com.ai.e2e.tx.example.gateway.common.filter.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;

/**
 * Filter that parses all headers from incoming request and puts all of those into Context.
 */
@Configuration
public class ContextSupplementaryHttpHeadersFilter implements WebFilter, Ordered {

  private static final int ORDER_OF_FILTER = 0;

  @Override
  public Mono<Void> filter(final ServerWebExchange serverWebExchange, final WebFilterChain webFilterChain) {
    return webFilterChain.filter(serverWebExchange)
        .subscriberContext(context -> putHttpHeadersToContext(serverWebExchange, context));
  }

  private Context putHttpHeadersToContext(final ServerWebExchange serverWebExchange, final Context context) {
    Context modifiedContext = context;
    for (final Map.Entry<String, List<String>> entry : serverWebExchange.getRequest().getHeaders().entrySet()) {
      modifiedContext = modifiedContext.put(entry.getKey(), entry.getValue());
    }
    return modifiedContext;
  }

  @Override
  public int getOrder() {
    return ORDER_OF_FILTER;
  }

}
