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

package org.apache.skywalking.apm.collector.storage.es.http.dao.register;

import java.util.HashMap;
import java.util.Map;

import org.apache.skywalking.apm.collector.client.elasticsearch.http.ElasticSearchHttpClient;
import org.apache.skywalking.apm.collector.storage.dao.register.IApplicationRegisterDAO;
import org.apache.skywalking.apm.collector.storage.es.http.base.dao.EsHttpDAO;
import org.apache.skywalking.apm.collector.storage.table.register.Application;
import org.apache.skywalking.apm.collector.storage.table.register.ApplicationTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cyberdak
 */
public class ApplicationRegisterEsDAO extends EsHttpDAO implements IApplicationRegisterDAO {

    private final Logger logger = LoggerFactory.getLogger(ApplicationRegisterEsDAO.class);

    public ApplicationRegisterEsDAO(ElasticSearchHttpClient client) {
        super(client);
    }

    @Override public int getMaxApplicationId() {
        return getMaxId(ApplicationTable.TABLE, ApplicationTable.COLUMN_APPLICATION_ID);
    }

    @Override public int getMinApplicationId() {
        return getMinId(ApplicationTable.TABLE, ApplicationTable.COLUMN_APPLICATION_ID);
    }

    @Override public void save(Application application) {
        logger.debug("save application register info, application getId: {}, application code: {}", application.getId(), application.getApplicationCode());
        ElasticSearchHttpClient client = getClient();
        Map<String, Object> source = new HashMap<>();
        source.put(ApplicationTable.COLUMN_APPLICATION_CODE, application.getApplicationCode());
        source.put(ApplicationTable.COLUMN_APPLICATION_ID, application.getApplicationId());
        source.put(ApplicationTable.COLUMN_ADDRESS_ID, application.getAddressId());
        source.put(ApplicationTable.COLUMN_IS_ADDRESS, application.getIsAddress());

        boolean response = client.prepareIndex(ApplicationTable.TABLE, application.getId(),source,true);
        logger.debug("save application register info, application getId: {}, application code: {}, status: {}", application.getApplicationId(), application.getApplicationCode(), response);
    }
}
