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

package org.apache.skywalking.oap.server.cluster.plugin.zookeeper;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceCacheBuilder;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.skywalking.oap.server.core.cluster.RemoteInstance;
import org.apache.skywalking.oap.server.core.remote.client.Address;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZookeeperCoordinatorTest {

    private ClusterModuleZookeeperConfig config = new ClusterModuleZookeeperConfig();

    private ServiceDiscovery<RemoteInstance> serviceDiscovery = mock(ServiceDiscovery.class);

    private ServiceCacheBuilder cacheBuilder = mock(ServiceCacheBuilder.class);

    private ServiceCache serviceCache = mock(ServiceCache.class);

    private ZookeeperCoordinator coordinator;

    private Address address = new Address("127.0.0.2", 10001, false);

    private Address selfAddress = new Address("127.0.0.1", 1000, true);

    private List<ServiceInstance<RemoteInstance>> cache = Lists.newArrayList();

    @Before
    public void setUp() throws Exception {
        when(cacheBuilder.name("remote")).thenReturn(cacheBuilder);
        when(cacheBuilder.build()).thenReturn(serviceCache);
        doNothing().when(serviceCache).start();

        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                cache.add((ServiceInstance<RemoteInstance>) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(serviceDiscovery).registerService(anyVararg());

        when(serviceCache.getInstances()).thenReturn(cache);

        when(serviceDiscovery.serviceCacheBuilder()).thenReturn(cacheBuilder);
        config.setHostPort(address.getHost() + ":" + address.getPort());
        coordinator = new ZookeeperCoordinator(config, serviceDiscovery);
    }

    @Test
    public void registerRemote() throws Exception {
        config.setInternalComHost(selfAddress.getHost());
        config.setInternalComPort(selfAddress.getPort());
        RemoteInstance instance = new RemoteInstance(address);
        coordinator.registerRemote(instance);
        validateServiceInstance(selfAddress, new RemoteInstance(selfAddress));
    }

    @Test
    public void updateNodes() throws Exception {
        String serverId = "1001";
        config.setInternalComHost(selfAddress.getHost());
        config.setInternalComPort(selfAddress.getPort());
        RemoteInstance instance = new RemoteInstance(address, serverId);
        coordinator.registerRemote(instance);
        RemoteInstance updateRemoteNodes = coordinator.updateRemoteNodes(serverId);
        assertEquals(updateRemoteNodes.getAddress(), selfAddress);
        assertEquals(updateRemoteNodes.getServerId(), serverId);
    }

    @Test
    public void registerRemoteNoNeedInternal() throws Exception {
        RemoteInstance instance = new RemoteInstance(address);
        coordinator.registerRemote(instance);
        validateServiceInstance(address, instance);
    }

    @SuppressWarnings("unchecked")
    private void validateServiceInstance(Address address, RemoteInstance instance) throws Exception {
        ArgumentCaptor<ServiceInstance> argumentCaptor = ArgumentCaptor.forClass(ServiceInstance.class);
        verify(serviceDiscovery).registerService(argumentCaptor.capture());

        ServiceInstance<RemoteInstance> serviceInstance = argumentCaptor.getValue();

        assertEquals("remote", serviceInstance.getName());
        assertTrue(!Strings.isNullOrEmpty(serviceInstance.getId()));
        assertEquals(address.getHost(), serviceInstance.getAddress());
        assertEquals(address.getPort(), serviceInstance.getPort().intValue());

        RemoteInstance payload = serviceInstance.getPayload();
        assertEquals(payload.getAddress(), address);

    }

    @Test
    public void queryRemoteNodes() {
    }
}