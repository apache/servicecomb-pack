package org.apache.servicecomb.pack.alpha.fsm.enums;

public enum  EventEnum {
    SagaStartedEvent("SagaStartedEvent"),
    SagaEndedEvent("SagaEndedEvent"),
    SagaAbortedEvent("SagaAbortedEvent"),
    SagaTimeoutEvent("SagaTimeoutEvent"),
    TxStartedEvent("TxStartedEvent"),
    TxEndedEvent("TxEndedEvent"),
    TxAbortedEvent("TxAbortedEvent"),
    TxComponsitedEvent("TxComponsitedEvent");

    private String event;

    EventEnum(String event) {
        this.event = event;
    }
}
