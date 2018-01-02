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

package org.apache.servicecomb.saga.demo.conditional.transaction.inventory;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(InventoryController.class)
public class InventoryControllerTest {
  private static final String content = "response=" + encode("{\n"
      + "  \"customerId\": \"mike\",\n"
      + "  \"foo\": \"bar\"\n"
      + "}");

  private static String encode(String param) {
    try {
      return URLEncoder.encode(param, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void respondWithChildren_IfStockIsLowerThanThreshold() throws Exception {
    mockMvc.perform(post("/inventory")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .content(content))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sagaChildren[0]", is("none")));

    mockMvc.perform(post("/inventory")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .content(content))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sagaChildren[0]").doesNotExist());
  }

  @Test
  public void badRequestIfCustomerIdIsNotInRequestContent() throws Exception {
    String content = "response=" + encode("{}");

    mockMvc.perform(post("/inventory")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .content(content))
        .andExpect(status().isBadRequest());
  }
}
