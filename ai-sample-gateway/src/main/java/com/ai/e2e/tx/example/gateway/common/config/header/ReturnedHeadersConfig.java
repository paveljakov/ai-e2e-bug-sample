package com.ai.e2e.tx.example.gateway.common.config.header;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses and serves headers that should be returned with response.
 */
@Component
public class ReturnedHeadersConfig extends AbstractHeadersFilterConfig {

  private final Map<String, String> staticReturnHeaders;

  public ReturnedHeadersConfig(@Value("${http.headers.return}") final Set<String> headersToReturn,
      @Value("${http.headers.return.static:}") final Set<String> staticResponseHeaders) {
    super(
        Stream.of(headersToReturn, defaultCorsHeaders())
            .flatMap(Collection::stream)
            .collect(Collectors.toSet())
    );

    staticReturnHeaders = staticResponseHeaders.stream()
        .map(header -> header.split(":"))
        .collect(Collectors.toMap(split -> split[0], split -> split.length == 1
                                                              ? ""
                                                              : split[1]));
  }

  private static Set<String> defaultCorsHeaders() {
    return Set.of(
        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
        HttpHeaders.ACCESS_CONTROL_MAX_AGE,
        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
    );

  }

  public Map<String, String> getStaticReturnHeaders() {
    return staticReturnHeaders;
  }
}
