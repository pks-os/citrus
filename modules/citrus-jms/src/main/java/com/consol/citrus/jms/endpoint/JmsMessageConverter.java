/*
 * Copyright 2006-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.jms.endpoint;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.message.MessageConverter;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.support.MessageBuilder;

import javax.jms.*;
import java.io.Serializable;
import java.util.*;

/**
 * Basic message converter for converting Spring Integration message implementations to JMS
 * messages and vice versa. Converter combines message converting logic and header mapping. 
 * Usually the message's payload is extracted to the JMS message payload and default JMS headers are mapped.
 * 
 * @author Christoph Deppisch
 */
public class JmsMessageConverter implements MessageConverter<javax.jms.Message> {

    /** The header mapper */
    private JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

    @Override
    public javax.jms.Message convertOutbound(Message<?> message) {
        throw new UnsupportedOperationException("Unable to create JMS message without JMS Session");
    }

    @Override
    public void convertOutbound(javax.jms.Message jmsMessage, Message<?> message) {
        MessageHeaders headers = message.getHeaders();

        if (headers != null) {
            headerMapper.fromHeaders(headers, jmsMessage);
        }
    }

    @Override
    public Message<?> convertInbound(javax.jms.Message jmsMessage) {
        if (jmsMessage == null) {
            return null;
        }

        try {
            Map<String, ?> headers = headerMapper.toHeaders(jmsMessage);
            Object payload;

            if (jmsMessage instanceof TextMessage) {
                payload = ((TextMessage) jmsMessage).getText();
            }
            else if (jmsMessage instanceof BytesMessage) {
                byte[] bytes = new byte[(int) ((BytesMessage) jmsMessage).getBodyLength()];
                ((BytesMessage) jmsMessage).readBytes(bytes);
                payload = bytes;
            }
            else if (jmsMessage instanceof MapMessage) {
                Map<String, Object> map = new HashMap<String, Object>();
                Enumeration en = ((MapMessage) jmsMessage).getMapNames();
                while (en.hasMoreElements()) {
                    String key = (String) en.nextElement();
                    map.put(key, ((MapMessage) jmsMessage).getObject(key));
                }
                payload = map;
            }
            else if (jmsMessage instanceof ObjectMessage) {
                payload = ((ObjectMessage) jmsMessage).getObject();
            }
            else {
                payload = jmsMessage;
            }

            return MessageBuilder.withPayload(payload)
                                            .copyHeaders(headers)
                                            .build();
        } catch (JMSException e) {
            throw new CitrusRuntimeException("Failed to convert jms message", e);
        }
    }

    /**
     * Creates JMS message instance from internal message representation. According to message payload type the JMS session
     * creates related JMS message type such as TextMessage, MapMessage, ObjectMessage or BytesMessage.
     *
     * @param message
     * @param session
     * @return
     */
    public javax.jms.Message createJmsMessage(Message<?> message, Session session) {
        try {
            Object payload = message.getPayload();

            javax.jms.Message jmsMessage;
            if (payload instanceof javax.jms.Message) {
                jmsMessage = (javax.jms.Message) payload;
            } else if (payload instanceof String) {
                jmsMessage = session.createTextMessage((String) payload);
            } else if (payload instanceof byte[]) {
                jmsMessage = session.createBytesMessage();
                ((BytesMessage)jmsMessage).writeBytes((byte[]) payload);
            } else if (payload instanceof Map) {
                jmsMessage = session.createMapMessage();
                Map<?, ?> map = ((Map) payload);
                for (Map.Entry entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String)) {
                        throw new CitrusRuntimeException("Cannot convert non-String key of type [" + entry.getKey() + "] to JMS MapMessage entry");
                    }
                    ((MapMessage)jmsMessage).setObject((String) entry.getKey(), entry.getValue());
                }
            } else if (payload instanceof Serializable) {
                jmsMessage = session.createObjectMessage((Serializable) payload);
            } else {
                throw new CitrusRuntimeException("Cannot convert object of type [" + payload + "] to JMS message. Supported message " +
                        "payloads are: String, byte array, Map<String,?>, Serializable object.");
            }
            convertOutbound(jmsMessage, message);

            return jmsMessage;
        } catch (JMSException e) {
            throw new CitrusRuntimeException("Failed to convert jms message", e);
        }
    }

    /**
     * Gets the JMS header mapper.
     * @return the headerMapper
     */
    public JmsHeaderMapper getHeaderMapper() {
        return headerMapper;
    }

    /**
     * Sets the JMS header mapper.
     * @param headerMapper the headerMapper to set
     */
    public void setHeaderMapper(JmsHeaderMapper headerMapper) {
        this.headerMapper = headerMapper;
    }
    
}