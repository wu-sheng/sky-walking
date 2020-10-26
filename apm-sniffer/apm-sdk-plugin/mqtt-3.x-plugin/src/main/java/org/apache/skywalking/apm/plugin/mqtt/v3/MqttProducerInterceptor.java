/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.mqtt.v3;

import java.lang.reflect.Method;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;

public class MqttProducerInterceptor implements InstanceMethodsAroundInterceptor {

    public static final String OPERATE_NAME_PREFIX = "Mqtt/";

    private static final String OPERATE_NAME = "/Producer/";

    @Override
    public void beforeMethod(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes,
                             MethodInterceptResult methodInterceptResult) throws Throwable {
        String topic;
        int qos;
        if (objects[0] instanceof MqttPublish) {
            MqttPublish mqttPublish = (MqttPublish) objects[0];
            topic = mqttPublish.getTopicName();
            qos = mqttPublish.getMessage().getQos();
        } else {
            return;
        }
        String operationName = OPERATE_NAME_PREFIX + topic + OPERATE_NAME + qos;
        MqttEnhanceRequiredInfo requiredInfo = (MqttEnhanceRequiredInfo) enhancedInstance.getSkyWalkingDynamicField();
        AbstractSpan activeSpan = ContextManager.createExitSpan(
            operationName, new ContextCarrier(), requiredInfo.getBrokerServers());
        Tags.MQ_BROKER.set(activeSpan, requiredInfo.getBrokerServers());
        Tags.MQ_TOPIC.set(activeSpan, topic);
        activeSpan.setLayer(SpanLayer.MQ);
        activeSpan.setComponent(ComponentsDefine.MQTT_PRODUCER);
    }

    @Override
    public Object afterMethod(EnhancedInstance enhancedInstance, Method method, Object[] objects, Class<?>[] classes,
                              Object o) throws Throwable {
        if (ContextManager.isActive()) {
            ContextManager.stopSpan();
        }
        return o;
    }

    @Override
    public void handleMethodException(EnhancedInstance enhancedInstance, Method method, Object[] objects,
                                      Class<?>[] classes, Throwable throwable) {
        ContextManager.activeSpan().log(throwable);
    }
}
