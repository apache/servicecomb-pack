package org.apache.servicecomb.saga.omega.transaction.tcc;

import javax.transaction.TransactionalException;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.EventAwareInterceptor;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.SagaEndedEvent;
import org.apache.servicecomb.saga.omega.transaction.SagaStartedEvent;
import org.apache.servicecomb.saga.omega.transaction.TxAbortedEvent;

public class TccStartAnnotationProcessor implements EventAwareInterceptor {

  private final OmegaContext omegaContext;
  private final MessageSender sender;

  TccStartAnnotationProcessor(OmegaContext omegaContext, MessageSender sender) {
    this.omegaContext = omegaContext;
    this.sender = sender;
  }

  @Override
  public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
      int retries, Object... message) {
    try {
      return sender.send(new SagaStartedEvent(omegaContext.globalTxId(), omegaContext.localTxId(), timeout));
    } catch (OmegaException e) {
      throw new TransactionalException(e.getMessage(), e.getCause());
    }
  }

  @Override
  public void postIntercept(String parentTxId, String compensationMethod) {
    // Send the confirm event
    /*AlphaResponse response = sender.send(new SagaEndedEvent(omegaContext.globalTxId(), omegaContext.localTxId()));
    if (response.aborted()) {
      throw new OmegaException("transaction " + parentTxId + " is aborted");
    }*/
  }

  @Override
  public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
    // Send the cancel event
    String globalTxId = omegaContext.globalTxId();
    sender.send(new TxAbortedEvent(globalTxId, omegaContext.localTxId(), null, compensationMethod, throwable));
  }
}
