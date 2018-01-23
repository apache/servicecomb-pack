Feature: Alpha records transaction events

  Scenario: Everything is normal
    Given Car Service is up and running
    And Hotel Service is up and running

    When A booking transaction is received

    Then Alpha records the following events
      | event type       |
      | SagaStartedEvent |
      | TxStartedEvent   |
      | TxEndedEvent     |
      | TxStartedEvent   |
      | TxEndedEvent     |
      | SagaEndedEvent   |
