package org.apache.servicecomb.pack.alpha.server.actuate.endpoint;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.actuate.endpoint.AlphaStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "endpoints.alpha")
@Component
public class AlphaEndpoint implements Endpoint {

  public static final String END_POINT_ID = "alpha";

  @Autowired
  NodeStatus nodeStatus;

  private AlphaStatus alphaStatus = new AlphaStatus();

  @Override
  public String getId() {
    return END_POINT_ID;
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
  public AlphaStatus invoke() {
    alphaStatus.setNodeType(nodeStatus.getTypeEnum());
    return alphaStatus;
  }

}
