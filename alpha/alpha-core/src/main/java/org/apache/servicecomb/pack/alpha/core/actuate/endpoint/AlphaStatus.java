package org.apache.servicecomb.pack.alpha.core.actuate.endpoint;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;

public class AlphaStatus {
  private NodeStatus.TypeEnum nodeType;

  public NodeStatus.TypeEnum getNodeType() {
    return nodeType;
  }

  public void setNodeType(NodeStatus.TypeEnum nodeType) {
    this.nodeType = nodeType;
  }
}
