package org.apache.servicecomb.pack.omega.transport.hystrix;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义包装hystrix callable 接口，便于处理线程变量等
 */
public interface HystrixCallableWrapper {

    /**
     * is need wrap
     *
     * @return return true will invoke  {@link #wrapCallable(Callable)}
     */
    boolean shouldWrap();

    /**
     * Provides an opportunity to wrap/decorate a {@code Callable<T>} before execution.
     * <p>
     * This can be used to inject additional behavior such as copying of thread state (such as {@link ThreadLocal}).
     *
     * @param callable {@code Callable<T>} to be executed via a {@link ThreadPoolExecutor}
     * @return {@code Callable<T>} either as a pass-thru or wrapping the one given
     */
    <T> Callable<T> wrapCallable(Callable<T> callable);

}
