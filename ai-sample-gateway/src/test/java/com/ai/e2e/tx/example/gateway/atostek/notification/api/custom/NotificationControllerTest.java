package com.ai.e2e.tx.example.gateway.atostek.notification.api.custom;

import com.atostek.generated.xml.Notification;
import com.atostek.generated.xml.NotificationResponse;
import com.atostek.generated.xml.PushNotificationResult;
import com.nortal.healthcare.gateway.atostek.notification.builder.NotificationResponseBuilder;
import com.nortal.healthcare.gateway.atostek.notification.handler.NotificationHandler;
import com.nortal.healthcare.gateway.atostek.notification.handler.factory.NotificationHandlerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NotificationControllerTest {

  @Mock
  private NotificationHandlerFactory notificationHandlerFactory;

  @Mock
  private Notification notification;

  @Mock
  private NotificationHandler notificationHandler1;

  @Mock
  private NotificationHandler notificationHandler2;

  @InjectMocks
  private NotificationController notificationController;

  @Mock
  private ServerWebExchange serverWebExchange;

  @Before
  public void setUp() throws Exception {
    when(serverWebExchange.getLogPrefix()).thenReturn(UUID.randomUUID().toString());
  }

  @Test
  public void handleNoHandlersFound() {
    when(notificationHandlerFactory.getNotificationHandlers(eq(notification))).thenReturn(Flux.empty());

    final Mono<NotificationResponse> notificationResponseMono =
        notificationController.handleNotification(serverWebExchange, Mono.just(notification), new HttpHeaders());
    final NotificationResponse notificationResponse = notificationResponseMono.block();

    assertEquals(PushNotificationResult.SUCCESS, notificationResponse.getResult());
  }

  @Test
  public void handleNotificationError() {
    when(notificationHandlerFactory.getNotificationHandlers(eq(notification)))
        .thenReturn(Flux.just(notificationHandler1, notificationHandler2));
    mockHandler(notificationHandler2, PushNotificationResult.PERMANENT_FAILURE);
    mockHandler(notificationHandler1, PushNotificationResult.SUCCESS);

    final Mono<NotificationResponse> notificationResponseMono =
        notificationController.handleNotification(serverWebExchange, Mono.just(notification), new HttpHeaders());
    final NotificationResponse notificationResponse = notificationResponseMono.block();

    assertEquals(PushNotificationResult.PERMANENT_FAILURE, notificationResponse.getResult());
  }

  @Test
  public void handleNotificationSuccess() {
    when(notificationHandlerFactory.getNotificationHandlers(eq(notification)))
        .thenReturn(Flux.just(notificationHandler1, notificationHandler2));
    mockHandler(notificationHandler2, PushNotificationResult.SUCCESS);
    mockHandler(notificationHandler1, PushNotificationResult.SUCCESS);

    final Mono<NotificationResponse> notificationResponseMono =
        notificationController.handleNotification(serverWebExchange, Mono.just(notification), new HttpHeaders());
    final NotificationResponse notificationResponse = notificationResponseMono.block();

    assertEquals(PushNotificationResult.SUCCESS, notificationResponse.getResult());
  }

  private void mockHandler(final NotificationHandler notificationHandler2,
      final PushNotificationResult permanentFailure) {
    when(notificationHandler2.handle(eq(notification), any())).thenAnswer(invocation -> {
      final NotificationResponseBuilder argument = invocation.getArgument(1);
      argument.addNotificationResult(permanentFailure, this.getClass());
      return Mono.just(argument);
    });
  }

}
