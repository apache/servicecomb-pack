package org.apache.servicecomb.saga.omega.transaction;

public class NoOpEventAwareInterceptor implements EventAwareInterceptor {

  public static final NoOpEventAwareInterceptor INSTANCE = new NoOpEventAwareInterceptor();

  @Override
  public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout,
      String retriesMethod,
      int retries, Object... message) {
    return new AlphaResponse(false);
  }

  @Override
  public void postIntercept(String parentTxId, String compensationMethod) {
    // NoOp
  }

  @Override
  public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
    // NoOp
  }
}
