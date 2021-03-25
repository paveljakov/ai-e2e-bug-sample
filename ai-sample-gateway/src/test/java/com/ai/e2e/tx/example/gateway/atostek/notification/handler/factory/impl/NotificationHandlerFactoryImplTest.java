package com.ai.e2e.tx.example.gateway.atostek.notification.handler.factory.impl;

import com.atostek.generated.xml.Notification;
import com.nortal.healthcare.gateway.atostek.notification.handler.NotificationHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NotificationHandlerFactoryImplTest {

  @Mock
  private NotificationHandler notificationHandler1;

  @Mock
  private NotificationHandler notificationHandler2;

  @Mock
  private NotificationHandler notificationHandler3;

  @Mock
  private Notification notification;

  private NotificationHandlerFactoryImpl notificationHandlerFactory;

  @Before
  public void setUp() throws Exception {
    notificationHandlerFactory = new NotificationHandlerFactoryImpl(List.of(
        notificationHandler1, notificationHandler2, notificationHandler3
    ));
  }

  @Test
  public void getNotificationHandlers() {
    when(notificationHandler1.supports(eq(notification))).thenReturn(Mono.just(true));
    when(notificationHandler2.supports(eq(notification))).thenReturn(Mono.just(false));
    when(notificationHandler3.supports(eq(notification))).thenReturn(Mono.just(true));

    final Flux<NotificationHandler> actualNotificationHandlersFlux =
        notificationHandlerFactory.getNotificationHandlers(notification);
    final List<NotificationHandler> notificationHandlers =
        actualNotificationHandlersFlux
            .collectList()
            .block();

    assertTrue(notificationHandlers.contains(notificationHandler1));
    assertFalse(notificationHandlers.contains(notificationHandler2));
    assertTrue(notificationHandlers.contains(notificationHandler3));
  }

}
