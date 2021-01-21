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

package org.apache.skywalking.apm.agent.core.pulsar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pulsar.client.api.Producer;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.apache.skywalking.apm.agent.core.boot.OverrideImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.os.OSUtil;
import org.apache.skywalking.apm.agent.core.pulsar.PulsarReporterPluginConfig.Plugin.Pulsar;
import org.apache.skywalking.apm.agent.core.remote.ServiceManagementClient;
import org.apache.skywalking.apm.network.common.v3.KeyStringValuePair;
import org.apache.skywalking.apm.network.management.v3.InstancePingPkg;
import org.apache.skywalking.apm.network.management.v3.InstanceProperties;
import org.apache.skywalking.apm.util.RunnableWithExceptionProtection;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * A service management data(Instance registering properties and Instance pinging) reporter.
 */
@OverrideImplementor(ServiceManagementClient.class)
public class PulsarServiceManagementServiceClient implements BootService, Runnable {

    private static final ILog LOGGER = LogManager.getLogger(PulsarServiceManagementServiceClient.class);

    private static List<KeyStringValuePair> SERVICE_INSTANCE_PROPERTIES;

    private static final String TOPIC_KEY_REGISTER = "register-";

    private ScheduledFuture<?> heartbeatFuture;
    private Producer<byte[]> producer;

    private AtomicInteger sendPropertiesCounter = new AtomicInteger(0);

    @Override
    public void prepare() {
        SERVICE_INSTANCE_PROPERTIES = new ArrayList<>();
        for (String key : Config.Agent.INSTANCE_PROPERTIES.keySet()) {
            SERVICE_INSTANCE_PROPERTIES.add(KeyStringValuePair.newBuilder()
                    .setKey(key)
                    .setValue(Config.Agent.INSTANCE_PROPERTIES.get(key))
                    .build());
        }

        Config.Agent.INSTANCE_NAME = StringUtil.isEmpty(Config.Agent.INSTANCE_NAME)
                ? UUID.randomUUID().toString().replaceAll("-", "") + "@" + OSUtil.getIPV4()
                : Config.Agent.INSTANCE_NAME;
    }

    @Override
    public void boot() {
        producer = ServiceManager.INSTANCE.findService(PulsarProducerManager.class)
                .getProducer(Pulsar.TOPIC_MANAGEMENT);

        heartbeatFuture = Executors.newSingleThreadScheduledExecutor(
                new DefaultNamedThreadFactory("ServiceManagementClientPulsarProducer")
        ).scheduleAtFixedRate(new RunnableWithExceptionProtection(
                this,
                t -> LOGGER.error("unexpected exception.", t)
        ), 0, Config.Collector.HEARTBEAT_PERIOD, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        if (Math.abs(sendPropertiesCounter.getAndAdd(1)) % Config.Collector.PROPERTIES_REPORT_PERIOD_FACTOR == 0) {
            InstanceProperties instance = InstanceProperties.newBuilder()
                    .setService(Config.Agent.SERVICE_NAME)
                    .setServiceInstance(Config.Agent.INSTANCE_NAME)
                    .addAllProperties(OSUtil.buildOSInfo(
                            Config.OsInfo.IPV4_LIST_SIZE))
                    .addAllProperties(SERVICE_INSTANCE_PROPERTIES)
                    .build();
            producer.newMessage()
                    .key(TOPIC_KEY_REGISTER + instance.getServiceInstance())
                    .value(instance.toByteArray())
                    .sendAsync();

        } else {
            InstancePingPkg ping = InstancePingPkg.newBuilder()
                    .setService(Config.Agent.SERVICE_NAME)
                    .setServiceInstance(Config.Agent.INSTANCE_NAME)
                    .build();
            if (LOGGER.isDebugEnable()) {
                LOGGER.debug("Heartbeat reporting, instance: {}", ping.getServiceInstance());
            }

            producer.newMessage()
                    .key(ping.getServiceInstance())
                    .value(ping.toByteArray())
                    .sendAsync();
        }
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void shutdown() {
        heartbeatFuture.cancel(true);
    }
}