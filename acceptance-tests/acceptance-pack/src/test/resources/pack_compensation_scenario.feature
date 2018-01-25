Feature: Alpha records transaction events

  Scenario: A sub-transaction failed and global transaction compensated
    Given Car Service is up and running
    And Hotel Service is up and running
    And Booking Service is up and running
    And Alpha is up and running

    When User Sean requests to book 5 cars and 3 rooms

    Then Car Service contains the following booking orders
      | name | amount | confirmed | cancelled |
      | Sean | 5      | false     | true      |

    Then Hotel Service contains the following booking orders
      | name | amount | confirmed | cancelled |

    Then Alpha records the following events
      | serviceName  | type               |
      | pack-booking | SagaStartedEvent   |
      | pack-car     | TxStartedEvent     |
      | pack-car     | TxEndedEvent       |
      | pack-hotel   | TxStartedEvent     |
      | pack-hotel   | TxAbortedEvent     |
      | pack-car     | TxCompensatedEvent |
      | pack-car     | SagaEndedEvent     |
