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
 *
 */

package org.apache.servicecomb.saga.omega.transport.feign;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.UUID;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.junit.Before;
import org.junit.Test;

public class FeignAutoConfigurationTest{

    private static final String globalTxId = UUID.randomUUID().toString();

    private static final String localTxId = UUID.randomUUID().toString();

    private final OmegaContext omegaContext = new OmegaContext(() -> "ignored");

    private RequestTemplate requestTemplate = new RequestTemplate(); // mock(RequestTemplate.class);

    private final RequestInterceptor feignClientRequestInterceptor = new FeignClientRequestInterceptor(omegaContext);

    @Before
    public void setUp() {
        omegaContext.clear();
    }

    @Test
    public void setUpOmegaContextInTransactionRequest() throws Exception {
        omegaContext.setGlobalTxId(globalTxId);
        omegaContext.setLocalTxId(localTxId);

        feignClientRequestInterceptor.apply(requestTemplate); // .preHandle(request, response, null);

        assertThat((new ArrayList(requestTemplate.headers().get(OmegaContext.GLOBAL_TX_ID_KEY))).get(0), is(globalTxId));
        assertThat((new ArrayList(requestTemplate.headers().get(OmegaContext.LOCAL_TX_ID_KEY))).get(0), is(localTxId));
    }

    @Test
    public void doNothingInNonTransactionRequest() throws Exception {
        omegaContext.setGlobalTxId(null);
        omegaContext.setLocalTxId(null);

        feignClientRequestInterceptor.apply(requestTemplate);

        assertThat(requestTemplate.headers().get(OmegaContext.GLOBAL_TX_ID_KEY), is(nullValue()));
        assertThat(requestTemplate.headers().get(OmegaContext.LOCAL_TX_ID_KEY), is(nullValue()));
    }
}