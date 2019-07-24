package org.apache.servicecomb.pack.alpha.fsm.channel.redis;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

public class RedisMessagePublisher implements MessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessagePublisher.class);

    private RedisTemplate<String, Object> redisTemplate;
    private ChannelTopic channelTopic;

    public RedisMessagePublisher(RedisTemplate<String, Object> redisTemplate, ChannelTopic channelTopic) {
        this.redisTemplate = redisTemplate;
        this.channelTopic = channelTopic;
    }

    @Override
    public void publish(Object data) {
        logger.info("send message [{}] to [{}]", data, channelTopic.getTopic());
        redisTemplate.convertAndSend(channelTopic.getTopic(), data);

    }
}
