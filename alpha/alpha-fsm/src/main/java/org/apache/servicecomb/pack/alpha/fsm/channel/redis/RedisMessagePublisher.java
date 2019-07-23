package org.apache.servicecomb.pack.alpha.fsm.channel.redis;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

public class RedisMessagePublisher implements MessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);

    private StringRedisTemplate stringRedisTemplate;
    private ChannelTopic channelTopic;

    public RedisMessagePublisher(StringRedisTemplate stringRedisTemplate, ChannelTopic channelTopic) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.channelTopic = channelTopic;
    }

    @Override
    public void publish(String message) {
        logger.info("send message [{}] to [{}]", message, channelTopic.getTopic());
        stringRedisTemplate.convertAndSend(channelTopic.getTopic(), message);

    }
}
