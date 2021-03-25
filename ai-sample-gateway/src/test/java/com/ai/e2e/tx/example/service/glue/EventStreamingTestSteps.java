package com.ai.e2e.tx.example.service.glue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nortal.healthcare.common.event.dto.atostek.AtostekEntityEnum;
import com.nortal.healthcare.common.event.dto.atostek.AtostekEntitySyncedEvent;
import com.nortal.healthcare.common.event.dto.change.ActionEnum;
import com.nortal.healthcare.common.event.dto.change.StateChangedEvent;
import com.nortal.healthcare.common.event.dto.encounter.EncounterCreatedEvent;
import com.nortal.healthcare.common.event.dto.service.event.ServiceEventOpenedEvent;
import com.nortal.healthcare.gateway.common.api.dto.StateChanged;
import com.nortal.healthcare.gateway.common.dto.SseContainer;
import com.nortal.healthcare.test.glue.BaseSteps;
import com.nortal.healthcare.test.services.TestKafkaService;
import com.nortal.healthcare.test.services.TestWiremockService;
import com.nortal.healthcare.test.services.auth.FakeJwtService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@RequiredArgsConstructor
public class EventStreamingTestSteps extends BaseSteps {

  private static final Map<UUID, String> EVENT_RESPONSE_MAP = new ConcurrentHashMap<>();
  private static final Map<UUID, StateChanged> STATE_CHANGED_EVENT_RESPONSE_MAP = new ConcurrentHashMap<>();
  private static final String FORM_SYNCED = "\"FORM_SYNCED\"";
  private static final String SERVICE_EVENT_OPENED = "{\"sseType\":\"SERVICE_EVENT_OPENED\",\"data\":";
  private static final String ARCHIVE_FINISHED = "\"ARCHIVE_FINISHED\"";
  private static final String PRESCRIPTION_SYNCED = "\"PRESCRIPTION_SYNCED\"";
  private static final String EMR_ATOSTEK_ENTITY_SYNCED = "emr.atostek-entity.synced";
  private static final String EMR_STATE_CHANGED = "emr.integration.entity-changed";
  private static final String EMR_SERVICE_EVENT_OPENED = "emr.service-event.opened";
  private static final String EMR_ARCHIVE_FINISHED = "emr.archive.finished";
  private static final List<String> SSE_MESSAGES_TO_IGNORE =
      List.of("{\"sseType\":\"[HEARTBEAT]\"}", "{\"sseType\":\"HEARTBEAT\"}", "\"HEARTBEAT\"", "\"[HEARTBEAT]\"");

  private final TestKafkaService testKafkaService;
  private final FakeJwtService fakeJwtService;
  private final TestWiremockService testWiremockService;

  private WebTestClient client;
  private UUID patientUuid;
  private UUID encounterUuid;
  private UUID entityUuid;
  private UUID practitionerUuid;
  private UUID openedServiceUuid;

  @When("patient exists")
  public void patientExists() {
    this.patientUuid = UUID.randomUUID();
  }

  @When("practitioner exists")
  public void practitionerExists() {
    this.practitionerUuid = UUID.randomUUID();
  }

  @When("encounter exists")
  public void encounterExists() {
    this.encounterUuid = UUID.randomUUID();
  }

  @When("entity exists")
  public void entityExists() {
    this.entityUuid = UUID.randomUUID();
  }

  @When("webTestClient is set up")
  public void setUp() {
    client = WebTestClient.bindToServer()
        .baseUrl("http://localhost:" + RestAssured.port)
        .responseTimeout(Duration.ofMillis(10000))
        .build();
  }

  @Given("Atostek form SSE is being listened to")
  public void formSseIsRunning() {
    final String format = String.format("/sse/form/synced/%s", patientUuid);
    subscribeTo(format, patientUuid);
  }

  @Given("Atostek call is mocked")
  public void mockAtostekCall() {
    this.openedServiceUuid = UUID.randomUUID();
    String format = String.format("/service/event/opened/encounter/%s", encounterUuid);
    mockAtostekCallValidation(format);
  }

  @Given("Service event opened SSE is being listened to")
  public void serviceEventSSEIsRunning() {
    final String format = String.format("/sse/service-event/opened/%s", encounterUuid);
    subscribeTo(format, encounterUuid);
  }

  @Given("Encounter Finished event SSE is being listened to")
  public void encounterSSEIsRunning() {
    final String format = String.format("/sse/encounter/created/%s", practitionerUuid);
    subscribeTo(format, practitionerUuid);
  }

  @Given("Prescription SSE is being listened to")
  public void prescriptionSseIsRunning() {
    final String format = String.format("/sse/prescription/synced/%s", patientUuid);
    subscribeTo(format, patientUuid);
  }

  @Given("a bundle of random events are emitted")
  public void aBundleOfRandomEventsAreEmitted() {
    IntStream.range(0, 500)
        .forEach((i) -> testKafkaService.sendMessage(EMR_STATE_CHANGED, getRandomStateChangedEvent()));
  }

  @Given("event {string} is emitted of certain encounter")
  public void eventIsEmittedOfCertainEncounter(final String entityType) {
    final StateChangedEvent stateChangedEvent = new StateChangedEvent();
    stateChangedEvent.setUuid(this.entityUuid);
    stateChangedEvent.setEntity(entityType);
    stateChangedEvent.setAction(ActionEnum.CREATED);
    stateChangedEvent.setEncounterUuid(this.encounterUuid);
    testKafkaService.sendMessage(EMR_STATE_CHANGED, stateChangedEvent);
  }

  @Given("state changed SSE is being listened to receive {string} of certain encounter")
  public void stateChangedSseIsBeingListenedTo(final String entityType) {
    client.get()
        .uri(uriBuilder -> uriBuilder
            .path("/sse/state-changed")
            .queryParam("filters[0].encounter", this.encounterUuid)
            .queryParam("filters[0].entity", entityType)
            .build()
        )
        .accept(TEXT_EVENT_STREAM)
        .header(HttpHeaders.AUTHORIZATION, fakeJwtService.getFakeJwt())
        .exchange()
        .expectStatus()
        .isOk()
        .returnResult(String.class)
        .getResponseBody()
        .filter(content -> shouldNotBeIgnored(content, SSE_MESSAGES_TO_IGNORE))
        .map(this::readSseContainer)
        .take(1)
        .subscribe(event -> STATE_CHANGED_EVENT_RESPONSE_MAP.put(event.getData().getUuid(), event.getData()));
  }

  @SneakyThrows
  private SseContainer<StateChanged> readSseContainer(final String content) {
    return new ObjectMapper().readValue(content, new TypeReference<>() {
    });
  }

  @Then("state changed SSE endpoint emits an event of {string} of certain encounter")
  public void stateChangedSseEndpointEmitsEventOfCertainEncounter(final String entityType) {
    Utils.retryFor(20, 500, () -> {
      final StateChanged stateChanged = STATE_CHANGED_EVENT_RESPONSE_MAP.get(entityUuid);
      assertEquals(entityType, stateChanged.getEntity());
      return null;
    });
  }

  @When("new Atostek form is received")
  public void formMessageIsSent() {
    testKafkaService.sendMessage(EMR_ATOSTEK_ENTITY_SYNCED,
        atostekEntitySyncedEvent(patientUuid, AtostekEntityEnum.FORM)
    );
  }

  @When("new Service event is opened")
  public void serviceEventIsSent() {
    testKafkaService.sendMessage(EMR_SERVICE_EVENT_OPENED, serviceEventOpenedEvent(encounterUuid, openedServiceUuid));
  }

  @When("Encounter is finished")
  public void encounterMessageIsSent() {

  }

  @When("new Prescription is received")
  public void prescriptionMessageIsSent() {
    testKafkaService.sendMessage(EMR_ATOSTEK_ENTITY_SYNCED,
        atostekEntitySyncedEvent(patientUuid, AtostekEntityEnum.PRESCRIPTION)
    );
  }

  @Then("Atostek form SSE endpoint emits an event")
  public void formMessageIsReceived() {
    final Supplier<String> eventSupplier = () -> verifyEvent(patientUuid, FORM_SYNCED);
    Utils.retryFor(20, 500, eventSupplier);
  }

  @Then("Service event SSE endpoint emits an event")
  public void serviceEventIsReceived() {
    final Supplier<String> eventSupplier = () -> verifyServiceEvent(encounterUuid);
    Utils.retryFor(20, 500, eventSupplier);
  }

  @Then("Prescription SSE endpoint emits an event")
  public void prescriptionMessageIsReceived() {
    final Supplier<String> eventSupplier = () -> verifyEvent(patientUuid, PRESCRIPTION_SYNCED);
    Utils.retryFor(20, 500, eventSupplier);
  }

  private void subscribeTo(final String uri, final UUID uuid) {

    client.get()
        .uri(uri)
        .accept(TEXT_EVENT_STREAM)
        .header(HttpHeaders.AUTHORIZATION, fakeJwtService.getFakeJwt())
        .exchange()
        .expectStatus()
        .isOk()
        .returnResult(String.class)
        .getResponseBody()
        .filter(content -> shouldNotBeIgnored(content, SSE_MESSAGES_TO_IGNORE))
        .take(1)
        .subscribe(event -> EVENT_RESPONSE_MAP.put(uuid, event));
  }

  private boolean shouldNotBeIgnored(final String content, final List<String> ignoredValues) {
    return ignoredValues.stream().noneMatch(content::equalsIgnoreCase);
  }

  private StateChangedEvent getRandomStateChangedEvent() {
    final StateChangedEvent stateChangedEvent = new StateChangedEvent();
    stateChangedEvent.setUuid(UUID.randomUUID());
    stateChangedEvent.setAction(randomEnum(ActionEnum.class));
    stateChangedEvent.setAppointmentUuid(UUID.randomUUID());
    stateChangedEvent.setClinicLocationUuid(UUID.randomUUID());
    stateChangedEvent.setEncounterUuid(UUID.randomUUID());
    stateChangedEvent.setPatientUuid(UUID.randomUUID());
    stateChangedEvent.setPractitionerUuid(UUID.randomUUID());
    stateChangedEvent.setEntity(getRandomEntity());
    return stateChangedEvent;
  }

  private String getRandomEntity() {
    final List<String> entities = List.of("ATOSTEK_PRESCRIPTION", "ATOSTEK_FORM", "ENCOUNTER", "ATOSTEK_SERVICE_EVENT");
    return entities.get(new Random().nextInt(entities.size()));
  }

  private <T extends Enum<?>> T randomEnum(final Class<T> clazz) {
    final int x = new Random().nextInt(clazz.getEnumConstants().length);
    return clazz.getEnumConstants()[x];
  }

  private AtostekEntitySyncedEvent atostekEntitySyncedEvent(final UUID patientUuid,
      final AtostekEntityEnum entityEnum) {
    final AtostekEntitySyncedEvent atostekEntitySyncedEvent = new AtostekEntitySyncedEvent();
    atostekEntitySyncedEvent.setPatientUuid(patientUuid);
    atostekEntitySyncedEvent.setEntity(entityEnum);
    return atostekEntitySyncedEvent;
  }

  private EncounterCreatedEvent encounterCreatedEvent(final UUID practitionerUuid) {
    final EncounterCreatedEvent encounterCreatedEvent = new EncounterCreatedEvent();
    encounterCreatedEvent.setPractitioner(practitionerUuid);
    encounterCreatedEvent.setUuid(encounterUuid);
    return encounterCreatedEvent;
  }

  private ServiceEventOpenedEvent serviceEventOpenedEvent(final UUID encounterUuid, final UUID serviceEvent) {
    final ServiceEventOpenedEvent serviceEventOpenedEvent = new ServiceEventOpenedEvent();
    serviceEventOpenedEvent.setEncounter(encounterUuid);
    serviceEventOpenedEvent.setServiceEvent(serviceEvent);
    return serviceEventOpenedEvent;
  }

  private String verifyEvent(final UUID uuid, final String expectedResponse) {
    final String entitySyncedEvent = EVENT_RESPONSE_MAP.get(uuid);
    assertNotNull(EVENT_RESPONSE_MAP.get(uuid));
    assertEquals(expectedResponse, EVENT_RESPONSE_MAP.get(uuid));
    return entitySyncedEvent;
  }

  private String verifyServiceEvent(final UUID uuid) {
    final String serviceEvent = EVENT_RESPONSE_MAP.get(uuid);
    assertNotNull(EVENT_RESPONSE_MAP.get(uuid));
    assertEquals(SERVICE_EVENT_OPENED + "\"" + openedServiceUuid + "\"}", EVENT_RESPONSE_MAP.get(uuid));
    return serviceEvent;
  }

  private void mockAtostekCallValidation(final String s) {
    testWiremockService.get().register(get(urlPathMatching(s))
        .willReturn(
            aResponse()
                .withBody("{\"message\": \"" + openedServiceUuid + "\"}")
                .withHeader("Content-Type", "application/json")
        )
    );
  }
}
