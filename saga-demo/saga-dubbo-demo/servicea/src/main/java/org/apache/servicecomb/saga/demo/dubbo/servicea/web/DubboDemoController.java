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

package org.apache.servicecomb.saga.demo.dubbo.servicea.web;

import org.apache.servicecomb.saga.demo.dubbo.api.IServiceA;
import org.apache.servicecomb.saga.demo.dubbo.pub.IInvokeCode;
import org.apache.servicecomb.saga.demo.dubbo.pub.InvokeContext;
import org.apache.servicecomb.saga.demo.dubbo.pub.ServiceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DubboDemoController {

  @Autowired
  private IServiceA serviceA;

    @javax.annotation.Resource(name = "jdbcTemplate")
    protected JdbcTemplate jdbcTemplate;


  @GetMapping("/serviceInvoke/Ab")
  public String serviceInvokeAb() throws Exception {
      serviceInvoke(IInvokeCode.Ab);
      String status_a = getServiceStatus("testa");
      String status_b = getServiceStatus("testb");
      boolean checkResult = checkResult("run", status_a, "run", status_b);
      return String.format(IInvokeCode.Ab_description +
              "</br>check result: %s" +
              "</br>expected status: A:run, B:run" +
              "</br> actual status: A:%s, B:%s", checkResult, status_a, status_b);
  }

    private boolean checkResult(String... statusArray) {
      for(int i=0; i<statusArray.length; i++){
          if(i%2==0 && i<statusArray.length){
              if(!statusArray[i].equals(statusArray[i+1])) return false;
          }
      }
      return true;
    }

    @GetMapping("/serviceInvoke/AExceptionWhenAb")
    public String serviceInvokeAExceptionWhenAb() throws Exception {
        serviceInvoke(IInvokeCode.AExceptionWhenAb);
        String status_a = getServiceStatus("testa");
        String status_b = getServiceStatus("testb");
        boolean checkResult = checkResult("init", status_a, "cancel", status_b);
        return String.format(IInvokeCode.AExceptionWhenAb_description +
                "</br>check result: %s" +
                "</br>expected status: A:init, B:cancel" +
                "</br> actual status: A:%s, B:%s", checkResult, status_a, status_b);
    }

    @GetMapping("/serviceInvoke/BExceptionWhenAb")
    public String serviceInvokeBExceptionWhenAb() throws Exception {
        serviceInvoke(IInvokeCode.BExceptionWhenAb);
        String status_a = getServiceStatus("testa");
        String status_b = getServiceStatus("testb");
        boolean checkResult = checkResult("init", status_a, "init", status_b);
        return String.format(IInvokeCode.BExceptionWhenAb_description +
                "</br>check result: %s" +
                "</br>expected status: A:init, B:init" +
                "</br> actual status: A:%s, B:%s", checkResult, status_a, status_b);
    }

    @GetMapping("/serviceInvoke/AbAc")
    public String serviceInvokeAbAc() throws Exception {
        serviceInvoke(IInvokeCode.AbAc);
        String status_a = getServiceStatus("testa");
        String status_b = getServiceStatus("testb");
        String status_c = getServiceStatus("testc");
        boolean checkResult = checkResult("run", status_a, "run", status_b, "run", status_c);
        return String.format(IInvokeCode.AbAc_description +
                "</br>check result: %s" +
                "</br>expected status: A:run, B:run, C:run" +
                "</br> actual status: A:%s, B:%s, C: %s", checkResult, status_a, status_b, status_c);
    }

    @GetMapping("/serviceInvoke/CExceptionWhenAbAc")
    public String serviceInvokeCExceptionWhenAbAc() throws Exception {
        serviceInvoke(IInvokeCode.CExceptionWhenAbAc);
        String status_a = getServiceStatus("testa");
        String status_b = getServiceStatus("testb");
        String status_c = getServiceStatus("testc");
        boolean checkResult = checkResult("init", status_a, "cancel", status_b, "init", status_c);
        return String.format(IInvokeCode.CExceptionWhenAbAc_description +
                "</br>check result: %s" +
                "</br>expected status: A:init, B:cancel, C:init" +
                "</br> actual status: A:%s, B:%s, C: %s", checkResult, status_a, status_b, status_c);
    }

    @GetMapping("/serviceInvoke/AbBc")
    public String serviceInvokeAbBc() throws Exception {
        serviceInvoke(IInvokeCode.AbBc);
        String status_a = getServiceStatus("testa");
        String status_b = getServiceStatus("testb");
        String status_c = getServiceStatus("testc");
        boolean checkResult = checkResult("run", status_a, "run", status_b, "run", status_c);
        return String.format(IInvokeCode.AbBc_description +
                "</br>check result: %s" +
                "</br>expected status: A:run, B:run, C:run" +
                "</br> actual status: A:%s, B:%s, C: %s", checkResult, status_a, status_b, status_c);
    }

    @GetMapping("/serviceInvoke/CExceptionWhenAbBc")
    public String serviceInvokeCExceptionWhenAbBc() throws Exception {
        serviceInvoke(IInvokeCode.CExceptionWhenAbBc);
        String status_a = getServiceStatus("testa");
        String status_b = getServiceStatus("testb");
        String status_c = getServiceStatus("testc");
        boolean checkResult = checkResult("init", status_a, "init", status_b, "init", status_c);
        return String.format(IInvokeCode.CExceptionWhenAbBc_description +
                "</br>check result: %s" +
                "</br>expected status: A:init, B:init, C:init" +
                "</br> actual status: A:%s, B:%s, C: %s", checkResult, status_a, status_b, status_c);
    }

    @GetMapping("/serviceInfo/{serviceName}")
    public List<ServiceVO> serviceInfo(@PathVariable String serviceName){
      String tableName = "test"+serviceName.replace("service", "");
        return this.jdbcTemplate.query("select * from " + tableName, new BeanPropertyRowMapper<>(ServiceVO.class));
    }

    public void serviceInvoke(String invokeCode) throws InterruptedException {
        reset();
        try{
            serviceA.run(new InvokeContext().setInvokeCode(invokeCode));
        }catch (Exception e){
            e.printStackTrace();
            //wait for compensate execute
            Thread.sleep(1000);
        }

    }

    public void reset() {
        this.jdbcTemplate.update("update testa set vstatus=? ", "init");
        this.jdbcTemplate.update("update testb set vstatus=? ", "init");
        this.jdbcTemplate.update("update testc set vstatus=? ", "init");
    }

    public String getServiceStatus(String tableName){
      return this.jdbcTemplate.queryForObject(String.format("select vstatus from %s", tableName), String.class);
    }

}
