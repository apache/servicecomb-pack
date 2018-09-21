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

  Scenario: Everything is normal
    Given Inventory Service is up and running
    And Payment Service is up and running
    And Ordering Service is up and running
    And Alpha is up and running

    When User UserA requests to order 2 units of ProductA with unit price 2 success

    Then Alpha records the following events
      | serviceName  | txType             |
      | ordering     | STARTED            |
      | inventory    | PARTICIPATED       |
      | payment      | PARTICIPATED       |
      | ordering     | ENDED              |
      | inventory    | COORDINATED        |
      | payment      | COORDINATED        |

    And Inventory Service contains the following booking orders
      | userName | productName | units | confirmed | cancelled |
      | UserA    | ProductA    |  2    | true      |  false    |

    And Payment Service contains the following booking orders
      | userName | amount | balance | confirmed | cancelled |
      | UserA    |   4    |  96     | true      | false     |
