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

package com.consol.citrus.validation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.Message;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.message.CitrusMessageHeaders;
import com.consol.citrus.validation.context.ValidationContext;

/**
 * Simple registry holding all available message validator implementations. Test context can ask this registry for
 * matching validator implementation according to the message type (e.g. xml, json, csv, plaintext).
 * 
 * Registry tries to find a matching validator for the message.
 * 
 * @author Christoph Deppisch
 */
public class MessageValidatorRegistry implements InitializingBean {

    /** List of registered message validator implementations */
    @Autowired(required = false)
    private List<MessageValidator<? extends ValidationContext>> messageValidators = 
                                    new ArrayList<MessageValidator<? extends ValidationContext>>();
    
    /**
     * Finds proper message validators for this message.
     *  
     * @param message the message to validate.
     * @return  the list of matching message validators.
     */
    public List<MessageValidator<? extends ValidationContext>> findMessageValidators(Message<?> message) {
        return findMessageValidators(message.getHeaders().get(CitrusMessageHeaders.MESSAGE_TYPE).toString());
    }


    /**
     * Finds matching message validators for this message type.
     * 
     * @param messageType the message type
     * @return the list of matching message validators.
     */
    public List<MessageValidator<? extends ValidationContext>> findMessageValidators(String messageType) {
        List<MessageValidator<? extends ValidationContext>> matchingValidators = new ArrayList<MessageValidator<? extends ValidationContext>>();
        
        for (MessageValidator<? extends ValidationContext> validator : messageValidators) {
            if (validator.supportsMessageType(messageType)) {
                matchingValidators.add(validator);
            }
        }
        
        if (matchingValidators.isEmpty()) {
            throw new CitrusRuntimeException("Could not find proper message validator for message type '" + 
                    messageType + "', please define a capable message validator for this message type");
        }
        
        return matchingValidators;
    }

    /**
     * Check if we have at least one message validator available.
     */
    public void afterPropertiesSet() throws Exception {
        if (messageValidators.isEmpty()) {
            throw new BeanCreationException("No message validators available in context - " +
            		"please spacify at leaest one message validator!");
        }
    }


    /**
     * Sets available message validator implementations.
     * @param messageValidators the messageValidators to set
     */
    public void setMessageValidators(
            List<MessageValidator<? extends ValidationContext>> messageValidators) {
        this.messageValidators = messageValidators;
    }
}
