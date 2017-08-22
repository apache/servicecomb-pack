/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.demo.hotel.reservation;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(HotelReservationController.class)
public class HotelReservationControllerTest {


  @Autowired
  private MockMvc mockMvc;

  @Test
  public void reservesHotel() throws Exception {
    mockMvc.perform(
        post("/reservations/")
            .contentType(APPLICATION_JSON_VALUE)
            .content("{\n"
                + "  \"customerId\": \"mike\"\n"
                + "}"))
        .andExpect(status().isOk())
        .andExpect(content().string(startsWith("Hotel reserved with id ")));
  }

  @Test
  public void cancelsHotelReservation() throws Exception {
    mockMvc.perform(
        delete("/reservations/")
            .contentType(APPLICATION_JSON_VALUE)
            .content("{\n"
                + "  \"customerId\": \"mike\"\n"
                + "}"))
        .andExpect(status().isOk())
        .andExpect(content().string(startsWith("Hotel reservation cancelled with id")));
  }
}