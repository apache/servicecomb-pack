package io.servicecomb.saga.spring;


import io.servicecomb.saga.core.application.SagaExecutionComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import java.lang.invoke.MethodHandles;

public class SagaShutdownListener implements ApplicationListener<ContextClosedEvent> {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        try {
            log.info("Stopping sagas");
            contextClosedEvent.getApplicationContext().getBean(SagaExecutionComponent.class).terminate();
            log.info("Stopped sagas successfully.");
        } catch (Exception ex) {
            log.error("Stopped sagas failed.", ex);
        }
    }
}
