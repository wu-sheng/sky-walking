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

package org.apache.skywalking.apm.collector.storage.es.http.dao.smp;

import org.apache.skywalking.apm.collector.client.elasticsearch.http.ElasticSearchHttpClient;
import org.apache.skywalking.apm.collector.core.storage.TimePyramid;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.storage.dao.smp.IServiceMonthMetricPersistenceDAO;
import org.apache.skywalking.apm.collector.storage.table.service.ServiceMetric;
import org.apache.skywalking.apm.collector.storage.table.service.ServiceMetricTable;

import io.searchbox.core.Index;
import io.searchbox.core.Update;

/**
 * @author peng-yongsheng
 */
public class ServiceMonthMetricEsPersistenceDAO extends AbstractServiceMetricEsPersistenceDAO implements IServiceMonthMetricPersistenceDAO<Index, Update, ServiceMetric> {

    public ServiceMonthMetricEsPersistenceDAO(ElasticSearchHttpClient client) {
        super(client);
    }

    @Override protected String tableName() {
        return ServiceMetricTable.TABLE + Const.ID_SPLIT + TimePyramid.Month.getName();
    }
}
