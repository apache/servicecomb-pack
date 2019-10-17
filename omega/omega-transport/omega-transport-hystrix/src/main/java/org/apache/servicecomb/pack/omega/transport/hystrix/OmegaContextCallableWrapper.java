package org.apache.servicecomb.pack.omega.transport.hystrix;

import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.servicecomb.pack.omega.context.OmegaContext;

/**
 * 处理omegaContext在开启hystrix情况下，线程变量传递问题
 */
public class OmegaContextCallableWrapper implements HystrixCallableWrapper {

    private OmegaContext context;

    public OmegaContextCallableWrapper(OmegaContext context) {
        this.context = context;
    }

    @Override
    public boolean shouldWrap() {
        return context != null && StringUtils.isNotEmpty(context.globalTxId());
    }

    @Override
    public <T> Callable<T> wrapCallable(Callable<T> callable) {
        return new WrappedCallable<>(callable, context.globalTxId(), context.localTxId(), context);
    }

    static class WrappedCallable<T> implements Callable<T> {

        private final Callable<T> target;

        private final String globalTxId;
        private final String localTxId;
        private final OmegaContext omegaContext;

        public WrappedCallable(Callable<T> target, String globalTxId, String localTxId, OmegaContext omegaContext) {
            this.target = target;
            this.omegaContext = omegaContext;
            this.globalTxId = globalTxId;
            this.localTxId = localTxId;
        }

        @Override
        public T call() throws Exception {
            try {
                omegaContext.setGlobalTxId(globalTxId);
                omegaContext.setLocalTxId(localTxId);
                return target.call();
            } finally {
                omegaContext.clear();
            }
        }
    }
}
