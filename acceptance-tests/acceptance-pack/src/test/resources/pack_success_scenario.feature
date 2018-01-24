Feature: Alpha records transaction events

  Scenario: Everything is normal
    Given Car Service is up and running
    And Hotel Service is up and running

    When User Sean requests to book 2 cars and 1 rooms

    Then Alpha records the following events
      | serviceName  | type             |
      | pack-booking | SagaStartedEvent |
      | pack-car     | TxStartedEvent   |
      | pack-car     | TxEndedEvent     |
      | pack-hotel   | TxStartedEvent   |
      | pack-hotel   | TxEndedEvent     |
      | pack-booking | SagaEndedEvent   |

    And Car Service contains the following booking orders
      | name | amount | confirmed | cancelled |
      | Sean | 2      | true      | false     |

    And Hotel Service contains the following booking orders
      | name | amount | confirmed | cancelled |
      | Sean | 1      | true      | false     |
