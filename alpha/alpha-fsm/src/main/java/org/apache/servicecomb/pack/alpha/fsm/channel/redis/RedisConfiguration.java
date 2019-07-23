package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

import org.apache.servicecomb.pack.alpha.fsm.sink.ActorEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@ConditionalOnProperty(value = "alpha.feature.akka.channel.type", havingValue = "redis")
@ConditionalOnClass(RedisConnection.class)
@Configuration
public class RedisConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    @Value("${alpha.feature.akka.channel.redis.topic:servicecomb-pack-actor-event}")
    private String topic;

    @Bean
    RedisMessageSubscriber redisMessageSubscriber(ActorEventSink actorEventSink){
        return new RedisMessageSubscriber(actorEventSink);
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(ActorEventSink actorEventSink){
        return new MessageListenerAdapter(redisMessageSubscriber(actorEventSink));
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier("redisConnectionFactory")RedisConnectionFactory redisConnectionFactory, ActorEventSink actorEventSink){
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();

        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        redisMessageListenerContainer.addMessageListener(redisMessageSubscriber(actorEventSink), channelTopic());

        return redisMessageListenerContainer;
    }

    @Bean
    MessagePublisher redisMessagePublisher(@Qualifier("stringRedisTemplate")StringRedisTemplate stringRedisTemplate){
        return new RedisMessagePublisher(stringRedisTemplate, channelTopic());
    }

    @Bean
    ChannelTopic channelTopic(){
        logger.info("build channel topic = [{}]", topic);
        return new ChannelTopic(topic);
    }

}
