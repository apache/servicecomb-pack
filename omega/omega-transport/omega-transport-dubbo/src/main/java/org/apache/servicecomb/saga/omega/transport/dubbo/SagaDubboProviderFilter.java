package org.apache.servicecomb.saga.omega.transport.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.rpc.*;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;

/**
 * get saga transaction id from dubbo invocation and set into omega context
* @author wuzq
* @email wuzunqian@msn.com
* @date 03/05/2018 10:44 AM
*/
@Activate(group = Constants.PROVIDER)
public class SagaDubboProviderFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        OmegaContext omegaContext = new SpringExtensionFactory().getExtension(OmegaContext.class, "omegaContext");
        String globalTxId = invocation.getAttachment(GLOBAL_TX_ID_KEY);
        if (globalTxId == null) {
            LOG.debug("no such omega context global id: {}", GLOBAL_TX_ID_KEY);
        }else{
            omegaContext.setGlobalTxId(globalTxId);
            omegaContext.setLocalTxId(invocation.getAttachment(LOCAL_TX_ID_KEY));
        }

        if(invoker != null){
            return invoker.invoke(invocation);
        }
        return null;
    }
}
