package org.apache.servicecomb.saga.omega.transport.dubbo;

import com.alibaba.dubbo.rpc.*;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;

public class DubboProviderFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(DubboProviderFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        log.debug("saga dubbo provider filter");
        OmegaContext omegaContext = SpringContext.getBean(OmegaContext.class);
        if (null == omegaContext){
            log.debug("omega context == null");
        }

        if (null != omegaContext){
            RpcContext rpcContext = RpcContext.getContext();
            if (null != rpcContext.getAttachment(GLOBAL_TX_ID_KEY)){
                omegaContext.setGlobalTxId(rpcContext.getAttachment(GLOBAL_TX_ID_KEY));
                omegaContext.setLocalTxId(rpcContext.getAttachment(LOCAL_TX_ID_KEY));
                log.debug("set omega context success");
            }
        }

        return invoker.invoke(invocation);
    }
}
