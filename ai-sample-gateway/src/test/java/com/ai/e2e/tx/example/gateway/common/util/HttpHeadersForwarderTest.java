package com.ai.e2e.tx.example.gateway.common.util;

import com.ai.e2e.tx.example.gateway.common.config.header.ForwardedHeadersConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpHeadersForwarderTest {

  private static final String KEY_1 = "key1";

  private static final String VALUE_1 = "value1";

  private static final String KEY_2 = "key2";

  private static final String VALUE_2 = "value2";

  @Mock
  private ForwardedHeadersConfig forwardedHeadersConfig;

  private HttpHeadersForwarder httpHeadersForwarder;

  @Before
  public void setUp() throws Exception {
    Set<Map.Entry<String, List<String>>> actualHeaders = new HashSet<>();
    actualHeaders.add(new AbstractMap.SimpleEntry<>(KEY_1, Collections.singletonList(VALUE_1)));
    actualHeaders.add(new AbstractMap.SimpleEntry<>(KEY_2, Collections.singletonList(VALUE_2)));
    httpHeadersForwarder = new HttpHeadersForwarder(forwardedHeadersConfig, actualHeaders);
  }

  @Test
  public void forward() {
    when(forwardedHeadersConfig.getHeadersLowerCase()).thenReturn(
        new HashSet<>(Arrays.asList(KEY_1.toLowerCase())));

    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeadersForwarder.accept(httpHeaders);

    assertEquals(1, httpHeaders.size());
    assertEquals(Arrays.asList(VALUE_1), httpHeaders.get(KEY_1));
  }

}
