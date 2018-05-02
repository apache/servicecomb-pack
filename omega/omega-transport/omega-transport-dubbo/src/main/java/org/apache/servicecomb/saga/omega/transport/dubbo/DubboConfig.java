package org.apache.servicecomb.saga.omega.transport.dubbo;

import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DubboConfig {
    @Bean
    public SpringContext springContext(){
        SpringContext springContext = new SpringContext();
        return springContext;
    }

    @Bean
    public ProviderConfig providerConfig(){
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setFilter("dubboProviderFilter");

        return providerConfig;
    }

    @Bean
    public ConsumerConfig consumerConfig(){
        ConsumerConfig consumerConfig = new ConsumerConfig();
        consumerConfig.setFilter("dubboConsumerFilter");

        return consumerConfig;
    }
}
