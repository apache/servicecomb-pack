package org.apache.servicecomb.pack.alpha.server.actuate.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActuatorEndpoint implements Endpoint<List<Endpoint>> {

  @Autowired
  private List<Endpoint> endpoints;

  @Override
  public String getId() {
    return "actuator";
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean isSensitive() {
    return false;
  }

  @Override
  public List<Endpoint> invoke() {
    throw new UnsupportedOperationException();
  }

  public List<Endpoint> endpoints() {
    return endpoints;
  }
}
