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

package org.apache.skywalking.oap.server.core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.CoreModuleConfig;
import org.apache.skywalking.oap.server.core.register.ServiceInstanceInventory;
import org.apache.skywalking.oap.server.core.storage.StorageModule;
import org.apache.skywalking.oap.server.core.storage.cache.IServiceInstanceInventoryCacheDAO;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.module.Service;
import org.apache.skywalking.oap.server.library.util.BooleanUtils;

import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class ServiceInstanceInventoryCache implements Service {

    private final ServiceInstanceInventory userServiceInstance;
    private final Cache<Integer, ServiceInstanceInventory> serviceInstanceIdCache;
    private final Cache<String, Integer> serviceInstanceNameCache;
    private final Cache<String, Integer> addressIdCache;
    private final ModuleManager moduleManager;
    private IServiceInstanceInventoryCacheDAO cacheDAO;

    public ServiceInstanceInventoryCache(ModuleManager moduleManager, CoreModuleConfig moduleConfig) {
        this.moduleManager = moduleManager;

        this.userServiceInstance = new ServiceInstanceInventory();
        this.userServiceInstance.setSequence(Const.USER_INSTANCE_ID);
        this.userServiceInstance.setName(Const.USER_CODE);
        this.userServiceInstance.setServiceId(Const.USER_SERVICE_ID);
        this.userServiceInstance.setIsAddress(BooleanUtils.FALSE);

        long initialSize = moduleConfig.getMaxSizeOfServiceInstanceInventory() / 10L;
        int initialCapacitySize = (int) (initialSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : initialSize);

        serviceInstanceIdCache = CacheBuilder.newBuilder()
            .initialCapacity(initialCapacitySize)
            .maximumSize(moduleConfig.getMaxSizeOfServiceInstanceInventory())
            .build();
        serviceInstanceNameCache = CacheBuilder.newBuilder()
            .initialCapacity(initialCapacitySize)
            .maximumSize(moduleConfig.getMaxSizeOfServiceInstanceInventory())
            .build();
        addressIdCache = CacheBuilder.newBuilder()
            .initialCapacity(initialCapacitySize)
            .maximumSize(moduleConfig.getMaxSizeOfServiceInstanceInventory())
            .build();
    }

    private IServiceInstanceInventoryCacheDAO getCacheDAO() {
        if (isNull(cacheDAO)) {
            this.cacheDAO = moduleManager.find(StorageModule.NAME)
                .provider()
                .getService(IServiceInstanceInventoryCacheDAO.class);
        }
        return this.cacheDAO;
    }

    public ServiceInstanceInventory get(int serviceInstanceId) {
        if (Const.USER_INSTANCE_ID == serviceInstanceId) {
            return userServiceInstance;
        }

        ServiceInstanceInventory serviceInstanceInventory = serviceInstanceIdCache.getIfPresent(serviceInstanceId);

        if (Objects.isNull(serviceInstanceInventory)) {
            serviceInstanceInventory = getCacheDAO().get(serviceInstanceId);
            if (Objects.nonNull(serviceInstanceInventory)) {
                serviceInstanceIdCache.put(serviceInstanceId, serviceInstanceInventory);
            }
        }
        return serviceInstanceInventory;
    }

    public int getServiceInstanceId(int serviceId, String uuid) {
        Integer serviceInstanceId = serviceInstanceNameCache.getIfPresent(ServiceInstanceInventory.buildId(serviceId, uuid));

        if (Objects.isNull(serviceInstanceId) || serviceInstanceId == Const.NONE) {
            serviceInstanceId = getCacheDAO().getServiceInstanceId(serviceId, uuid);
            if (serviceId != Const.NONE) {
                serviceInstanceNameCache.put(ServiceInstanceInventory.buildId(serviceId, uuid), serviceInstanceId);
            }
        }
        return serviceInstanceId;
    }

    public int getServiceInstanceId(int serviceId, int addressId) {
        Integer serviceInstanceId = addressIdCache.getIfPresent(ServiceInstanceInventory.buildId(serviceId, addressId));

        if (Objects.isNull(serviceInstanceId) || serviceInstanceId == Const.NONE) {
            serviceInstanceId = getCacheDAO().getServiceInstanceId(serviceId, addressId);
            if (serviceId != Const.NONE) {
                addressIdCache.put(ServiceInstanceInventory.buildId(serviceId, addressId), serviceInstanceId);
            }
        }
        return serviceInstanceId;
    }

    public String getServiceInstanceLanguage(int serviceInstanceId) {
        ServiceInstanceInventory inventory = get(serviceInstanceId);
        if (isNull(inventory)) {
            return Const.EMPTY_STRING;
        }
        String language = inventory.getLanguage();
        if (nonNull(language)) {
            return language;
        }
        JsonObject properties = inventory.getProperties();
        for (String key : properties.keySet()) {
            if (key.equals(ServiceInstanceInventory.PropertyUtil.LANGUAGE)) {
                language = properties.get(key).getAsString();
                inventory.setLanguage(language);
                return language;
            }
        }
        inventory.setLanguage(Const.UNKNOWN);
        return Const.UNKNOWN;
    }
}
