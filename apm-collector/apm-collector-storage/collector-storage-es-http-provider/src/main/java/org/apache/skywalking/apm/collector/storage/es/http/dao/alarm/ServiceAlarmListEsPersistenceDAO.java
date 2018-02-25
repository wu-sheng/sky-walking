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

package org.apache.skywalking.apm.collector.storage.es.http.dao.alarm;

import java.util.HashMap;
import java.util.Map;

import org.apache.skywalking.apm.collector.client.elasticsearch.http.ElasticSearchHttpClient;
import org.apache.skywalking.apm.collector.core.util.TimeBucketUtils;
import org.apache.skywalking.apm.collector.storage.dao.alarm.IServiceAlarmListPersistenceDAO;
import org.apache.skywalking.apm.collector.storage.es.http.base.dao.EsHttpDAO;
import org.apache.skywalking.apm.collector.storage.table.alarm.ServiceAlarmList;
import org.apache.skywalking.apm.collector.storage.table.alarm.ServiceAlarmListTable;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Update;

/**
 * @author peng-yongsheng
 */
public class ServiceAlarmListEsPersistenceDAO extends EsHttpDAO implements IServiceAlarmListPersistenceDAO<Index, Update, ServiceAlarmList> {

    private final Logger logger = LoggerFactory.getLogger(ServiceAlarmListEsPersistenceDAO.class);

    public ServiceAlarmListEsPersistenceDAO(ElasticSearchHttpClient client) {
        super(client);
    }

    @Override public ServiceAlarmList get(String id) {
        DocumentResult getResponse = getClient().prepareGet(ServiceAlarmListTable.TABLE, id);
        if (getResponse.isSucceeded()) {
            ServiceAlarmList serviceAlarmList = new ServiceAlarmList();
            serviceAlarmList.setId(id);
            JsonObject source = getResponse.getSourceAsObject(JsonObject.class);
            serviceAlarmList.setApplicationId((source.get(ServiceAlarmListTable.COLUMN_APPLICATION_ID)).getAsInt());
            serviceAlarmList.setInstanceId((source.get(ServiceAlarmListTable.COLUMN_INSTANCE_ID)).getAsInt());
            serviceAlarmList.setServiceId((source.get(ServiceAlarmListTable.COLUMN_SERVICE_ID)).getAsInt());
            serviceAlarmList.setSourceValue((source.get(ServiceAlarmListTable.COLUMN_SOURCE_VALUE)).getAsInt());

            serviceAlarmList.setAlarmType((source.get(ServiceAlarmListTable.COLUMN_ALARM_TYPE)).getAsInt());
            serviceAlarmList.setAlarmContent(source.get(ServiceAlarmListTable.COLUMN_ALARM_CONTENT).getAsString());

            serviceAlarmList.setTimeBucket((source.get(ServiceAlarmListTable.COLUMN_TIME_BUCKET)).getAsLong());
            return serviceAlarmList;
        } else {
            return null;
        }
    }

    @Override public Index prepareBatchInsert(ServiceAlarmList data) {
        Map<String, Object> source = new HashMap<>();
        source.put(ServiceAlarmListTable.COLUMN_APPLICATION_ID, data.getApplicationId());
        source.put(ServiceAlarmListTable.COLUMN_INSTANCE_ID, data.getInstanceId());
        source.put(ServiceAlarmListTable.COLUMN_SERVICE_ID, data.getServiceId());
        source.put(ServiceAlarmListTable.COLUMN_SOURCE_VALUE, data.getSourceValue());

        source.put(ServiceAlarmListTable.COLUMN_ALARM_TYPE, data.getAlarmType());
        source.put(ServiceAlarmListTable.COLUMN_ALARM_CONTENT, data.getAlarmContent());

        source.put(ServiceAlarmListTable.COLUMN_TIME_BUCKET, data.getTimeBucket());

        return new Index.Builder(source).index(ServiceAlarmListTable.TABLE).id(data.getId()).build();
    }

    @Override public Update prepareBatchUpdate(ServiceAlarmList data) {
        Map<String, Object> source = new HashMap<>();
        source.put(ServiceAlarmListTable.COLUMN_APPLICATION_ID, data.getApplicationId());
        source.put(ServiceAlarmListTable.COLUMN_INSTANCE_ID, data.getInstanceId());
        source.put(ServiceAlarmListTable.COLUMN_SERVICE_ID, data.getServiceId());
        source.put(ServiceAlarmListTable.COLUMN_SOURCE_VALUE, data.getSourceValue());

        source.put(ServiceAlarmListTable.COLUMN_ALARM_TYPE, data.getAlarmType());
        source.put(ServiceAlarmListTable.COLUMN_ALARM_CONTENT, data.getAlarmContent());

        source.put(ServiceAlarmListTable.COLUMN_TIME_BUCKET, data.getTimeBucket());

        return new Update.Builder(source).index(ServiceAlarmListTable.TABLE).id(data.getId()).build();
    }

    @Override public void deleteHistory(Long startTimestamp, Long endTimestamp) {
        long startTimeBucket = TimeBucketUtils.INSTANCE.getMinuteTimeBucket(startTimestamp);
        long endTimeBucket = TimeBucketUtils.INSTANCE.getMinuteTimeBucket(endTimestamp);
        
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.rangeQuery(ServiceAlarmListTable.COLUMN_TIME_BUCKET).gte(startTimeBucket).lte(endTimeBucket));

        long deleted = getClient().batchDelete(ServiceAlarmListTable.TABLE, searchSourceBuilder.toString());
        logger.info("Delete {} rows history from {} index.", deleted, ServiceAlarmListTable.TABLE);
    }
}
