package org.apache.servicecomb.saga.omega.transport.dubbo;

import com.alibaba.dubbo.rpc.*;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;

public class DubboConsumerFilter implements Filter {
    private static Logger log = LoggerFactory.getLogger(DubboConsumerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        log.debug("saga dubbo consumer filter");
        OmegaContext omegaContext = SpringContext.getBean(OmegaContext.class);
        if (null == omegaContext){
            log.debug("omega context == null");
        }

        RpcContext rpcContext = RpcContext.getContext();
        if (null != omegaContext && omegaContext.globalTxId() != null) {
            rpcContext.setAttachment(GLOBAL_TX_ID_KEY, omegaContext.globalTxId());
            rpcContext.setAttachment(LOCAL_TX_ID_KEY, omegaContext.localTxId());

            log.debug("Added {} {} and {} {} to request header",
                    GLOBAL_TX_ID_KEY,
                    omegaContext.globalTxId(),
                    LOCAL_TX_ID_KEY,
                    omegaContext.localTxId());
        }

        return invoker.invoke(invocation);
    }
}
