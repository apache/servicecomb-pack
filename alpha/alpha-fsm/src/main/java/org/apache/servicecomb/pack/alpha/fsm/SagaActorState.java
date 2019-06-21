package org.apache.servicecomb.pack.alpha.fsm;

import akka.persistence.fsm.PersistentFSM;

public enum SagaActorState implements PersistentFSM.FSMState {
  IDEL,
  READY,
  PARTIALLY_ACTIVE,
  PARTIALLY_COMMITTED,
  FAILED,
  COMMITTED,
  COMPENSATED,
  SUSPENDED;

  @Override
  public String identifier() {
    return name();
  }
}
