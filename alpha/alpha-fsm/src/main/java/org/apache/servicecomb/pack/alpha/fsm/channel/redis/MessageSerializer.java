package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MessageSerializer {

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

            }

            return null;
        }
    }
}
