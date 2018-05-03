//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.servicecomb.saga.omega.transport.dubbo;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.lang.invoke.MethodHandles;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * add saga transaction id to dubbo invocation
* @author wuzq
* @email wuzunqian@msn.com
* @date 03/05/2018 10:44 AM
*/
@Activate(
        group = {"consumer"}
)
public class SagaDubboConsumerFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        OmegaContext omegaContext = (OmegaContext) (new SpringExtensionFactory()).getExtension(OmegaContext.class, "omegaContext");
        if (omegaContext.globalTxId() != null) {
            invocation.getAttachments().put("X-Pack-Global-Transaction-Id", omegaContext.globalTxId());
            invocation.getAttachments().put("X-Pack-Local-Transaction-Id", omegaContext.localTxId());
            LOG.debug("Added {} {} and {} {} to dubbo invocation", new Object[]{"X-Pack-Global-Transaction-Id", omegaContext.globalTxId(), "X-Pack-Local-Transaction-Id", omegaContext.localTxId()});
        }

        if (invoker != null) {
            return invoker.invoke(invocation);
        }
        return null;
    }
}
