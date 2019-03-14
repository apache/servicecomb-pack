package org.apache.servicecomb.pack.alpha.server.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;

@Configuration
@ConditionalOnProperty(value = {"spring.cloud.consul.enabled"})
@AutoConfigureAfter(ConsulAutoConfiguration.class)
public class AlphaConsulAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private ConsulClient consulClient;

  @Value("${spring.cloud.consul.discovery.instanceId}")
  private String consuleInstanceId;

  @PostConstruct
  public void init(){
    // Unregister from consul when shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        String remoteConsuleInstanceId = getRemoteConsulInstanceId();
        LOG.info("Unregister Consul {}", remoteConsuleInstanceId);
        consulClient.agentServiceDeregister(remoteConsuleInstanceId);
      }
    });
  }

  /**
   * Format local consul instanceId to instanceId of consul server
   * */
  private String getRemoteConsulInstanceId() {
    return consuleInstanceId.replace(".", "-").replace(":", "-");
  }
}
