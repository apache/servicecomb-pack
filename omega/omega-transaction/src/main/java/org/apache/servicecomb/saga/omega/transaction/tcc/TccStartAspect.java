package org.apache.servicecomb.saga.omega.transaction.tcc;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.apache.servicecomb.saga.omega.context.annotations.TccStart;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class TccStartAspect {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TccStartAnnotationProcessor tccStartAnnotationProcessor;

  private final OmegaContext context;

  public TccStartAspect(MessageSender sender, OmegaContext context) {
    this.context = context;
    this.tccStartAnnotationProcessor = new TccStartAnnotationProcessor(context, sender);
  }

  @Around("execution(@org.apache.servicecomb.saga.omega.context.annotations.TccStart * *(..)) && @annotation(tccStart)")
  Object advise(ProceedingJoinPoint joinPoint, TccStart tccStart) throws Throwable {
    initializeOmegaContext();
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

    tccStartAnnotationProcessor.preIntercept(context.globalTxId(), method.toString(), tccStart.timeout(), "", 0);
    LOG.debug("Initialized context {} before execution of method {}", context, method.toString());

    try {
      Object result = joinPoint.proceed();

      tccStartAnnotationProcessor.postIntercept(context.globalTxId(), method.toString());
      LOG.debug("Transaction with context {} has finished.", context);

      return result;
    } catch (Throwable throwable) {
      // We don't need to handle the OmegaException here
      if (!(throwable instanceof OmegaException)) {
        tccStartAnnotationProcessor.onError(context.globalTxId(), method.toString(), throwable);
        LOG.error("Transaction {} failed.", context.globalTxId());
      }
      throw throwable;
    } finally {
      context.clear();
    }
  }

  private void initializeOmegaContext() {
    context.setLocalTxId(context.newGlobalTxId());
  }
}
