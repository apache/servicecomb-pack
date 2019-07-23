package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.servicecomb.pack.alpha.fsm.enums.EventEnum;
import org.apache.servicecomb.pack.alpha.fsm.event.*;
import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHelper {

    private static final Logger logger = LoggerFactory.getLogger(EventHelper.class);

    public static BaseEvent getEvent(String message){
        JSONObject jsonObject = JSON.parseObject(message);

        String eventName = jsonObject.getString("eventName");
        String data = jsonObject.getString("data");

        BaseEvent baseEvent = null;

        EventEnum eventEnum = EventEnum.valueOf(eventName);

        switch (eventEnum){
            case SagaStartedEvent:
                baseEvent = JSON.parseObject(data, SagaStartedEvent.class);
                break;
            case SagaEndedEvent:
                baseEvent = JSON.parseObject(data, SagaEndedEvent.class);
                break;
            case SagaAbortedEvent:
                baseEvent = JSON.parseObject(data, SagaAbortedEvent.class);
                break;
            case SagaTimeoutEvent:
                baseEvent = JSON.parseObject(data, SagaTimeoutEvent.class);
                break;
            case TxStartedEvent:
                baseEvent = JSON.parseObject(data, TxStartedEvent.class);
                break;
            case TxEndedEvent:
                baseEvent = JSON.parseObject(data, TxEndedEvent.class);
                break;
            case TxAbortedEvent:
                baseEvent = JSON.parseObject(data, TxAbortedEvent.class);
                break;
            case TxComponsitedEvent:
                baseEvent = JSON.parseObject(data, TxCompensatedEvent.class);
                break;
            default:
                logger.warn("getEvent eventEvent is not match");
                break;

        }

        return baseEvent;
    }

    public static String getMessage(BaseEvent baseEvent){
        EventEnum eventEnum = null;
        if(baseEvent instanceof SagaStartedEvent){
            eventEnum = EventEnum.SagaStartedEvent;
        }else if(baseEvent instanceof SagaEndedEvent){
            eventEnum = EventEnum.SagaEndedEvent;
        }else if (baseEvent instanceof SagaAbortedEvent){
            eventEnum = EventEnum.SagaAbortedEvent;
        }else if (baseEvent instanceof SagaTimeoutEvent){
            eventEnum = EventEnum.SagaTimeoutEvent;
        }else if (baseEvent instanceof  TxStartedEvent){
            eventEnum = EventEnum.TxStartedEvent;
        }else if (baseEvent instanceof  TxEndedEvent){
            eventEnum = EventEnum.TxEndedEvent;
        }else if (baseEvent instanceof  TxAbortedEvent){
            eventEnum = EventEnum.TxAbortedEvent;
        }else if (baseEvent instanceof TxCompensatedEvent){
            eventEnum = EventEnum.TxComponsitedEvent;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("eventName",eventEnum.name());
        jsonObject.put("data", JSON.toJSONString(baseEvent));

        return jsonObject.toJSONString();
    }
}
