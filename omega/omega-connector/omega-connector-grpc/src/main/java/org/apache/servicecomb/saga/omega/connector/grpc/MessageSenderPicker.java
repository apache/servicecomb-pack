/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import com.google.common.base.Supplier;
import java.util.Collection;
import java.util.Map;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;

/**
 * The strategy of picking a specific {@link MessageSender} from a {@link Collection} of {@link
 * MessageSender}s
 */
public interface MessageSenderPicker {

  /**
   * Pick one from the Collection. Return default sender if none is picked.
   *
   * @param messageSenders Candidates map, the Key Set of which is the collection of candidate
   * senders.
   * @param defaultSender Default sender provider
   * @return The specified one.
   */
  MessageSender pick(Map<MessageSender, Long> messageSenders,
      Supplier<MessageSender> defaultSender);
}
