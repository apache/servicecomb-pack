package org.apache.servicecomb.pack.alpha.fsm;

public enum TxState {
  ACTIVE,
  FAILED,
  COMMITTED,
  COMPENSATED;
}
