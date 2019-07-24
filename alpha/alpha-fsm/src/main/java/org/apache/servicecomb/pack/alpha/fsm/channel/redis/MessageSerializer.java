package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

public class MessageSerializer {

    private static final Logger logger = LoggerFactory.getLogger(MessageSerializer.class);

    private static MessageSerializerImpl serializer = null;

    private MessageSerializer() {
        serializer = new MessageSerializerImpl();
    }

    public static Optional<byte[]> serializer(Object data){
        return Optional.ofNullable(serializer.serialize(data));
    }

    public static Optional<Object> deserialize(byte[] bytes){
        return Optional.ofNullable(serializer.deserialize(bytes));
    }

    private class MessageSerializerImpl implements RedisSerializer<Object>{
        @Override
        public byte[] serialize(Object data) throws SerializationException {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
                outputStream.writeObject(data);

                byte[] bytes = byteArrayOutputStream.toByteArray();

                outputStream.close();

                return bytes;
            }catch (Exception e){
                logger.error("serialize Exception = [{}]", e);
            }

            return null;
        }

        @Override
        public Object deserialize(byte[] bytes) throws SerializationException {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

                Object object = objectInputStream.readObject();

                objectInputStream.close();

                return object;
            }catch (Exception e){
                logger.error("deserialize Exception = [{}]", e);
            }

            return null;
        }
    }
}
