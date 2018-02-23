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

package org.apache.skywalking.apm.collector.storage.es.http.dao.ui;

import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;

import io.searchbox.core.DocumentResult;

import java.util.Base64;
import java.util.Map;

import org.apache.skywalking.apm.collector.client.elasticsearch.http.ElasticSearchHttpClient;
import org.apache.skywalking.apm.collector.core.util.StringUtils;
import org.apache.skywalking.apm.collector.storage.dao.ui.ISegmentUIDAO;
import org.apache.skywalking.apm.collector.storage.es.http.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.segment.SegmentTable;
import org.apache.skywalking.apm.network.proto.TraceSegmentObject;
import org.elasticsearch.action.get.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class SegmentEsUIDAO extends EsDAO implements ISegmentUIDAO {

    private final Logger logger = LoggerFactory.getLogger(SegmentEsUIDAO.class);

    public SegmentEsUIDAO(ElasticSearchHttpClient client) {
        super(client);
    }

    @Override public TraceSegmentObject load(String segmentId) {
        DocumentResult response = getClient().prepareGet(SegmentTable.TABLE, segmentId);
        JsonObject source = response.getJsonObject().getAsJsonObject("_source");
        String dataBinaryBase64 = source.get(SegmentTable.COLUMN_DATA_BINARY).getAsString();
        if (StringUtils.isNotEmpty(dataBinaryBase64)) {
            byte[] dataBinary = Base64.getDecoder().decode(dataBinaryBase64);
            try {
                return TraceSegmentObject.parseFrom(dataBinary);
            } catch (InvalidProtocolBufferException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
