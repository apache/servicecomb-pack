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

package org.apache.servicecomb.pack.alpha.ui;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.pack.alpha.ui.vo.SystemInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.Sample;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController implements ErrorController {

  @Autowired
  MetricsEndpoint metricsEndpoint;

  // index
  @GetMapping("/admin")
  public String index(ModelMap map) {
    map.put("systemInfo",getSystemInfo());
    return "index";
  }

  @GetMapping("/ui/saga")
  public String sagaIndex() {
    return "saga";
  }

  @GetMapping("/ui/tcc")
  public String tccIndex() {
    return "tcc";
  }

  @GetMapping("/ui/search")
  public String searchIndex(ModelMap map, @RequestParam(name = "globalTxId") String globalTxId) {
    map.put("globalTxId", globalTxId);
    return "search";
  }

  @GetMapping("/error")
  public String handleError() {
    return "error";
  }

  @Override
  public String getErrorPath() {
    return "/error";
  }

  private SystemInfoDTO getSystemInfo() {
    SystemInfoDTO systemInfoDTO = new SystemInfoDTO();

    //uptime
    long startTime = metricsEndpoint.metric("process.start.time", null).getMeasurements().get(0)
        .getValue().longValue();
    long seconds = System.currentTimeMillis() / 1000 - startTime;
    int day = (int) TimeUnit.SECONDS.toDays(seconds);
    long hours = TimeUnit.SECONDS.toHours(seconds) - (day * 24);
    long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60);
    long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60);
    systemInfoDTO.setUpTime(String.format("%dd %dh %dm %ds", day, hours, minute, second));

    //cpus
    systemInfoDTO.setCpus(
        metricsEndpoint.metric("system.cpu.count", null).getMeasurements().get(0).getValue()
            .intValue());

    //system load
    systemInfoDTO.setSystemLoad(Math.round(
        metricsEndpoint.metric("system.load.average.1m", null).getMeasurements().get(0).getValue()
            .floatValue() * 100) / 100);

    //thread
    systemInfoDTO.setThreadsLive(
        metricsEndpoint.metric("jvm.threads.live", null).getMeasurements().get(0).getValue()
            .intValue());
    systemInfoDTO.setThreadsDaemon(
        metricsEndpoint.metric("jvm.threads.daemon", null).getMeasurements().get(0).getValue()
            .intValue());
    systemInfoDTO.setThreadsPeak(
        metricsEndpoint.metric("jvm.threads.peak", null).getMeasurements().get(0).getValue()
            .intValue());

    //gc
    List<Sample> samples = metricsEndpoint.metric("jvm.gc.pause", null).getMeasurements();
    systemInfoDTO.setGcCount(samples.get(0).getValue().intValue());
    systemInfoDTO.setGcTime(samples.get(1).getValue().floatValue());
    return systemInfoDTO;
  }
}
