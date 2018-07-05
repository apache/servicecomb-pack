package org.apache.servicecomb.saga.omega.transport.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;

/**
 * 增加Feign拦截器，实现spring cloud下feign调用传递全局事务和本地事务。
 * create by lionel on 2018/07/05
 */
public class FeignClientRequestInterceptor implements RequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OmegaContext omegaContext;

    public FeignClientRequestInterceptor(OmegaContext omegaContext) {
        this.omegaContext = omegaContext;
    }

    @Override
    public void apply(RequestTemplate input) {
        if (omegaContext!= null && omegaContext.globalTxId() != null) {
            input.header(GLOBAL_TX_ID_KEY, omegaContext.globalTxId());
            input.header(LOCAL_TX_ID_KEY, omegaContext.localTxId());

            LOG.debug("Added {} {} and {} {} to request header",
                    GLOBAL_TX_ID_KEY,
                    omegaContext.globalTxId(),
                    LOCAL_TX_ID_KEY,
                    omegaContext.localTxId());
        }
    }
}
