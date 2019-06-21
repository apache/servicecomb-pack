package org.apache.servicecomb.pack.alpha.fsm.event;

public class SagaDomainEvent {
  public interface DomainEvent {}

  public enum SagaStartedEvent implements DomainEvent {INSTANCE}
  public enum SagaEndedEvent implements DomainEvent {INSTANCE}
  public enum SagaAbortedEvent implements DomainEvent {INSTANCE}
  public enum SagaTimeoutEvent implements DomainEvent {INSTANCE}
  public enum TxStartedEvent implements DomainEvent {INSTANCE}
  public enum TxEndedEvent implements DomainEvent {INSTANCE}
  public enum TxAbortedEvent implements DomainEvent {INSTANCE}
  public enum TxComponsitedEvent implements DomainEvent {INSTANCE}
}
