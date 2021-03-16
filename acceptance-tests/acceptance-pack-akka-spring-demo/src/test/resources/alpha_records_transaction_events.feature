# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

Feature: Alpha records transaction events

  Scenario: 1.Success Scenario
    Given Car Service is up and running
    And Hotel Service is up and running
    And Booking Service is up and running
    And Alpha is up and running

    When User Sean requests to book 2 cars and 1 rooms success

    Then Alpha records the following events
      | serviceName  | type             |
      | booking | SagaStartedEvent |
      | car     | TxStartedEvent   |
      | car     | TxEndedEvent     |
      | hotel   | TxStartedEvent   |
      | hotel   | TxEndedEvent     |
      | booking | SagaEndedEvent   |

    And Car Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 2      | true      | false     |

    And Hotel Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 1      | true      | false     |


  Scenario: 2.Booking Exception After Car Hotel Secenario
    Given Car Service is up and running
    And Hotel Service is up and running
    And Booking Service is up and running
    And Alpha is up and running

    Given Install the byteman script booking_exception_after_post_car.btm to Booking Service

    When User Sean requests to book 1 cars and 1 rooms fail

    Then Alpha records the following events
      | serviceName  | type               |
      | booking | SagaStartedEvent   |
      | car     | TxStartedEvent     |
      | car     | TxEndedEvent       |
      | booking | SagaAbortedEvent     |
      | car     | TxCompensateAckSucceedEvent |

    Then Car Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 1      | false     | true      |

    Then Hotel Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |

  Scenario: 2.Booking Exception After Post Hotel Secenario
    Given Car Service is up and running
    And Hotel Service is up and running
    And Booking Service is up and running
    And Alpha is up and running

    Given Install the byteman script booking_exception_after_post_hotel.btm to Booking Service

    When User Sean requests to book 1 cars and 1 rooms fail

    Then Alpha records the following events
      | serviceName | type               |
      | booking     | SagaStartedEvent   |
      | car         | TxStartedEvent     |
      | car         | TxEndedEvent       |
      | hotel       | TxStartedEvent     |
      | hotel       | TxEndedEvent       |
      | booking     | SagaAbortedEvent   |
      | hotel       | TxCompensateAckSucceedEvent |
      | car         | TxCompensateAckSucceedEvent |

    Then Car Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 1      | false     | true      |

    Then Hotel Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 1      | false     | true      |

  Scenario: 4.Booking transaction timeout and will be suspended
    Given Car Service is up and running
    And Hotel Service is up and running
    And Booking Service is up and running
    And Alpha is up and running

    Given Install the byteman script booking_timeout.btm to Booking Service

    When User Sean requests to book 1 cars and 1 rooms fail

    Then Alpha records the following events
      | serviceName  | type          |
      | booking | SagaStartedEvent   |
      | car     | TxStartedEvent     |
      | car     | TxEndedEvent       |
      | hotel   | TxStartedEvent     |
      | hotel   | TxEndedEvent       |


    Then Car Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 1      | true     | false      |

    Then Hotel Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 1      | true     | false      |

  Scenario: 5.Car sub-transaction failed and global transaction compensated
    Given Car Service is up and running
    And Hotel Service is up and running
    And Booking Service is up and running
    And Alpha is up and running

    When User Sean requests to book 11 cars and 1 rooms fail

    Then Alpha records the following events
      | serviceName  | type          |
      | booking | SagaStartedEvent   |
      | car     | TxStartedEvent     |
      | car     | TxAbortedEvent     |
      | booking | SagaAbortedEvent   |

    Then Car Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |

    Then Hotel Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |

  Scenario: 6.Hotel sub-transaction failed and global transaction compensated
    Given Car Service is up and running
    And Hotel Service is up and running
    And Booking Service is up and running
    And Alpha is up and running

    When User Sean requests to book 5 cars and 3 rooms fail

    Then Alpha records the following events
      | serviceName  | type               |
      | booking | SagaStartedEvent   |
      | car     | TxStartedEvent     |
      | car     | TxEndedEvent       |
      | hotel   | TxStartedEvent     |
      | hotel   | TxAbortedEvent     |
      | booking | SagaAbortedEvent     |
      | car     | TxCompensateAckSucceedEvent |

    Then Car Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |
      | 1  | Sean | 5      | false     | true      |

    Then Hotel Service contains the following booking orders
      | id | name | amount | confirmed | cancelled |

#  This scenario occasionally fail on poor performance CI.
#  Did not receive the third compensation within the waiting time
#  I created a JIRA https://issues.apache.org/jira/browse/SCB-2204 for it
#
#  Scenario: 7.Hotel sub-transaction failed and Car sub-transaction compensate failed and global transaction suspended
#    Given Car Service is up and running
#    And Hotel Service is up and running
#    And Booking Service is up and running
#    And Alpha is up and running
#    Given Install the byteman script car_compensate_failed_retry.btm to Car Service
#    When User Sean requests to book 5 cars and 3 rooms fail
#    Then Alpha records the following events
#      | serviceName  | type         |
#      | booking | SagaStartedEvent  |
#      | car     | TxStartedEvent    |
#      | car     | TxEndedEvent      |
#      | hotel   | TxStartedEvent    |
#      | hotel   | TxAbortedEvent    |
#      | booking | SagaAbortedEvent  |
#      | car     | TxCompensateAckFailedEvent |
#      | car     | TxCompensateAckFailedEvent |
#      | car     | TxCompensateAckFailedEvent |
#
#    Then Car Service contains the following booking orders
#      | id | name | amount | confirmed | cancelled |
#      | 1  | Sean | 5      | true     | false      |
#
#    Then Hotel Service contains the following booking orders
#      | id | name | amount | confirmed | cancelled |
