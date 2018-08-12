package org.apache.servicecomb.saga.omega.transaction.tcc;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.CompensableInterceptor;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.RecoveryPolicy;
import org.apache.servicecomb.saga.omega.transaction.RecoveryPolicyFactory;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class TccAspect {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final OmegaContext context;

  private final CompensableInterceptor interceptor;

  public TccAspect(MessageSender sender, OmegaContext context) {
    this.context = context;
    this.interceptor = new CompensableInterceptor(context, sender);
  }

  @Around("execution(@org.apache.servicecomb.saga.omega.transaction.annotations.Compensable * *(..)) && @annotation(compensable)")
  Object advise(ProceedingJoinPoint joinPoint, Compensable compensable) throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    String localTxId = context.localTxId();
    context.newLocalTxId();
    LOG.debug("Updated context {} for compensable method {} ", context, method.toString());

    int retries = compensable.retries();
    RecoveryPolicy recoveryPolicy = RecoveryPolicyFactory.getRecoveryPolicy(retries);
    try {
      return recoveryPolicy.apply(joinPoint, compensable, interceptor, context, localTxId, retries);
    } finally {
      context.setLocalTxId(localTxId);
      LOG.debug("Restored context back to {}", context);
    }
  }
}
