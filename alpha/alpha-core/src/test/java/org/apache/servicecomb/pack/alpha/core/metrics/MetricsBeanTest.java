package org.apache.servicecomb.pack.alpha.core.metrics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MetricsBeanTest {

  @Test
  public void testEventRecevie(){
    MetricsBean metric = new MetricsBean();
    //accepted
    metric.doEventReceived();
    metric.doEventAccepted();
    assertEquals(metric.getEventReceived(),1l);
    assertEquals(metric.getEventAccepted(),1l);
    //rejected
    metric.doEventReceived();
    metric.doEventRejected();
    assertEquals(metric.getEventReceived(),2l);
    assertEquals(metric.getEventAccepted(),1l);
    assertEquals(metric.getEventRejected(),1l);
  }

  @Test
  public void testActorReceive(){
    MetricsBean metric = new MetricsBean();
    //accepted
    metric.doActorReceived();
    metric.doActorAccepted();
    assertEquals(metric.getActorReceived(),1l);
    assertEquals(metric.getActorAccepted(),1l);
    //rejected
    metric.doActorReceived();
    metric.doActorRejected();
    assertEquals(metric.getActorReceived(),2l);
    assertEquals(metric.getActorAccepted(),1l);
    assertEquals(metric.getActorRejected(),1l);
  }

  @Test
  public void testRepositoryReceive(){
    MetricsBean metric = new MetricsBean();
    //accepted
    metric.doRepositoryReceived();
    metric.doRepositoryAccepted();
    assertEquals(metric.getRepositoryReceived(),1l);
    assertEquals(metric.getRepositoryAccepted(),1l);
    //rejected
    metric.doRepositoryReceived();
    metric.doRepositoryRejected();
    assertEquals(metric.getRepositoryReceived(),2l);
    assertEquals(metric.getRepositoryAccepted(),1l);
    assertEquals(metric.getRepositoryRejected(),1l);
  }

}
