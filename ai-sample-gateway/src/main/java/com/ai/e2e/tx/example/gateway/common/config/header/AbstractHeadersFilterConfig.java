package com.ai.e2e.tx.example.gateway.common.config.header;

import java.util.Set;
import java.util.stream.Collectors;

public class AbstractHeadersFilterConfig {

  private final Set<String> headers;

  public AbstractHeadersFilterConfig(final Set<String> headers) {
    this.headers = headers.stream()
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }

  public Set<String> getHeadersLowerCase() {
    return headers;
  }

}
