package com.ai.e2e.tx.example.gateway.common.util;

import com.ai.e2e.tx.example.gateway.common.config.header.AbstractHeadersFilterConfig;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Utility class that puts allowed to pass headers into HttpHeaders of request or response.
 */
public class HttpHeadersForwarder implements Consumer<HttpHeaders> {

  private final AbstractHeadersFilterConfig abstractHeadersFilterConfig;

  private final Set<Map.Entry<String, List<String>>> incomingHeaders;

  public HttpHeadersForwarder(final AbstractHeadersFilterConfig abstractHeadersFilterConfig,
      final Set<Map.Entry<String, List<String>>> incomingHeaders) {
    this.abstractHeadersFilterConfig = abstractHeadersFilterConfig;
    this.incomingHeaders = incomingHeaders;
  }

  @Override
  public void accept(final HttpHeaders outgoingHttpHeaders) {
    forwardSpecifiedHeaders(outgoingHttpHeaders, abstractHeadersFilterConfig.getHeadersLowerCase());
  }

  private void forwardSpecifiedHeaders(
      final HttpHeaders outgoingHttpHeaders, final Set<String> headersToForwardLowerCase
  ) {
    this.incomingHeaders.forEach(incomingHeader -> {
      if (incomingHeaderShouldBeForwarded(headersToForwardLowerCase, incomingHeader)
          && headerIsNotForwardedYet(outgoingHttpHeaders, incomingHeader)
      ) {
        outgoingHttpHeaders.addAll(incomingHeader.getKey(), incomingHeader.getValue());
      }
    });
  }

  private boolean headerIsNotForwardedYet(
      final HttpHeaders outgoingHttpHeaders,
      final Map.Entry<String, List<String>> incomingHeader) {
    return !outgoingHttpHeaders.containsKey(incomingHeader.getKey());
  }

  private boolean incomingHeaderShouldBeForwarded(
      final Set<String> headersToForwardLowerCase,
      final Map.Entry<String, List<String>> incomingHeader
  ) {
    return headersToForwardLowerCase.contains(incomingHeader.getKey().toLowerCase(Locale.getDefault()));
  }

}
