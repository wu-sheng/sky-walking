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

package org.apache.skywalking.apm.collector.storage.es.http.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.skywalking.apm.collector.client.elasticsearch.http.ElasticSearchHttpClient;
import org.apache.skywalking.apm.collector.core.UnexpectedException;
import org.apache.skywalking.apm.collector.storage.dao.IInstanceHeartBeatPersistenceDAO;
import org.apache.skywalking.apm.collector.storage.es.http.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.register.Instance;
import org.apache.skywalking.apm.collector.storage.table.register.InstanceTable;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.searchbox.core.DocumentResult;

/**
 * @author peng-yongsheng
 */
public class InstanceHeartBeatEsPersistenceDAO extends EsDAO implements IInstanceHeartBeatPersistenceDAO<IndexRequestBuilder, UpdateRequestBuilder, Instance> {

    private final Logger logger = LoggerFactory.getLogger(InstanceHeartBeatEsPersistenceDAO.class);

    public InstanceHeartBeatEsPersistenceDAO(ElasticSearchHttpClient client) {
        super(client);
    }

    @Override public Instance get(String id) {
        DocumentResult getResponse = getClient().prepareGet(InstanceTable.TABLE, id);
        if (getResponse.isSucceeded()) {
            JsonObject source = getResponse.getJsonObject().getAsJsonObject("_source");

            Instance instance = new Instance();
            instance.setId(id);
            instance.setInstanceId((source.get(InstanceTable.COLUMN_INSTANCE_ID)).getAsInt());
            instance.setHeartBeatTime((source.get(InstanceTable.COLUMN_HEARTBEAT_TIME)).getAsLong());
            logger.debug("getId: {} is exists", id);
            return instance;
        } else {
            logger.debug("getId: {} is not exists", id);
            return null;
        }
    }

    @Override public IndexRequestBuilder prepareBatchInsert(Instance data) {
        throw new UnexpectedException("There is no need to merge stream data with database data.");
    }

    @Override public UpdateRequestBuilder prepareBatchUpdate(Instance data) {
        Map<String, Object> source = new HashMap<>();
        source.put(InstanceTable.COLUMN_HEARTBEAT_TIME, data.getHeartBeatTime());
        return getClient().prepareUpdate(InstanceTable.TABLE, data.getId()).setDoc(source);
    }

    @Override public void deleteHistory(Long startTimestamp, Long endTimestamp) {
    }
}
