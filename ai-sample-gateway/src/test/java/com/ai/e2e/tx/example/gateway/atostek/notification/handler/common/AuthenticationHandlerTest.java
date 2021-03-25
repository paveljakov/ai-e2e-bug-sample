package com.ai.e2e.tx.example.gateway.atostek.notification.handler.common;

import com.atostek.generated.xml.Notification;
import com.atostek.generated.xml.NotificationResponse;
import com.atostek.generated.xml.PushNotificationResult;
import com.nortal.healthcare.gateway.atostek.notification.builder.NotificationResponseBuilder;
import com.nortal.healthcare.store.ExternalConfigJksKeyStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationHandlerTest {

  private static final String DATA_TO_SIGN = "any data that has to be signed";

  private static final String LOG_PREFIX = UUID.randomUUID().toString();

  @Mock
  private ExternalConfigJksKeyStore atostekKeyStore;

  @InjectMocks
  private AuthenticationHandler authenticationHandler;

  @Mock
  private Notification notification;

  @Before
  public void setUp() throws Exception {
    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(512);
    final KeyPair keyPair = keyGen.generateKeyPair();
    when(atostekKeyStore.getPublicKey()).thenReturn(keyPair.getPublic());
    when(atostekKeyStore.getPrivateKey()).thenReturn(keyPair.getPrivate());
  }

  @Test
  public void handle() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    when(notification.getSignatureData()).thenReturn(DATA_TO_SIGN);

    final Mono<NotificationResponseBuilder> notificationResponseBuilderMono =
        authenticationHandler.handle(notification, new NotificationResponseBuilder(LOG_PREFIX));

    final NotificationResponse notificationResponse = notificationResponseBuilderMono.block().build();
    assertEquals(PushNotificationResult.SUCCESS, notificationResponse.getResult());
    verifySignature(notificationResponse.getSignature());
  }

  @Test
  public void handleError() {
    when(notification.getSignatureData()).thenReturn(DATA_TO_SIGN);
    when(atostekKeyStore.getPrivateKey()).thenReturn(null);

    final Mono<NotificationResponseBuilder> notificationResponseBuilderMono =
        authenticationHandler.handle(notification, new NotificationResponseBuilder(LOG_PREFIX));

    final NotificationResponse notificationResponse = notificationResponseBuilderMono.block().build();
    assertEquals(PushNotificationResult.PERMANENT_FAILURE, notificationResponse.getResult());
  }

  private void verifySignature(final String signatureString)
      throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
    final Signature signature = Signature.getInstance(AuthenticationHandler.SIGNATURE_ALGORITHM);
    signature.initVerify(atostekKeyStore.getPublicKey());
    signature.update(DATA_TO_SIGN.getBytes());
    assertTrue(signature.verify(Base64.getDecoder().decode(signatureString)));
  }

}
