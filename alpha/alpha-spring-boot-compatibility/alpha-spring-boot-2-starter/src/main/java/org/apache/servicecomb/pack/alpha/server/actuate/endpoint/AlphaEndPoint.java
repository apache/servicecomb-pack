package org.apache.servicecomb.pack.alpha.server.actuate.endpoint;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.actuate.endpoint.AlphaStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;


@Configuration
@Endpoint(id = "alpha")
public class AlphaEndPoint {

  private AlphaStatus alphaStatus = new AlphaStatus();

  @Autowired
  @Lazy
  private NodeStatus nodeStatus;

  @ReadOperation
  public AlphaStatus endpoint() {
    alphaStatus.setNodeType(nodeStatus.getTypeEnum());
    return alphaStatus;
  }
}
