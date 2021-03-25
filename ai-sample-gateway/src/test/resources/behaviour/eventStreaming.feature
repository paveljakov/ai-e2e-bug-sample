Feature: Event Streaming

  Background:
    When patient exists
    And practitioner exists
    And encounter exists
    And webTestClient is set up

  Scenario: Form Synced Event Streaming
    Given Atostek form SSE is being listened to
    When new Atostek form is received
    Then Atostek form SSE endpoint emits an event

  Scenario: Service Event Opened Event Streaming
    Given Atostek call is mocked
    And Service event opened SSE is being listened to
    When new Service event is opened
    Then Service event SSE endpoint emits an event

  Scenario: Prescription Synced Event Streaming
    Given Prescription SSE is being listened to
    When new Prescription is received
    Then Prescription SSE endpoint emits an event

  Scenario: State changed event streaming for prescription
    Given entity exists
    And state changed SSE is being listened to receive "ATOSTEK_PRESCRIPTION" of certain encounter
    And a bundle of random events are emitted
    And event "ATOSTEK_PRESCRIPTION" is emitted of certain encounter
    Then state changed SSE endpoint emits an event of "ATOSTEK_PRESCRIPTION" of certain encounter
