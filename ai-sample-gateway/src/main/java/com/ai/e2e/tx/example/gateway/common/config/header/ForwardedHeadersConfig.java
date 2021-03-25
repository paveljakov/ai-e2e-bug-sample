package com.ai.e2e.tx.example.gateway.common.config.header;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Parses and serves headers that should be forwarded downstream.
 */
@Component
public class ForwardedHeadersConfig extends AbstractHeadersFilterConfig {

  public ForwardedHeadersConfig(@Value("${http.headers.forward}") final Set<String> headersToForward) {
    super(headersToForward);
  }

}
