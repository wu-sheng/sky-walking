/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
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
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.collector.storage.es.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.skywalking.apm.collector.core.util.ColumnNameUtils;
import org.skywalking.apm.collector.storage.dao.INodeReferenceUIDAO;
import org.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.skywalking.apm.collector.storage.table.noderef.NodeReferenceTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class NodeReferenceEsUIDAO extends EsDAO implements INodeReferenceUIDAO {

    private final Logger logger = LoggerFactory.getLogger(NodeReferenceEsUIDAO.class);

    public NodeReferenceEsUIDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override public JsonArray load(long startTime, long endTime) {
        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(NodeReferenceTable.TABLE);
        searchRequestBuilder.setTypes(NodeReferenceTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchRequestBuilder.setQuery(QueryBuilders.rangeQuery(NodeReferenceTable.COLUMN_TIME_BUCKET).gte(startTime).lte(endTime));
        searchRequestBuilder.setSize(0);

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID).field(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID).size(100);
        aggregationBuilder.subAggregation(AggregationBuilders.terms(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID).field(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID).size(100)
            .subAggregation(AggregationBuilders.sum(NodeReferenceTable.COLUMN_S1_LTE).field(NodeReferenceTable.COLUMN_S1_LTE))
            .subAggregation(AggregationBuilders.sum(NodeReferenceTable.COLUMN_S3_LTE).field(NodeReferenceTable.COLUMN_S3_LTE))
            .subAggregation(AggregationBuilders.sum(NodeReferenceTable.COLUMN_S5_LTE).field(NodeReferenceTable.COLUMN_S5_LTE))
            .subAggregation(AggregationBuilders.sum(NodeReferenceTable.COLUMN_S5_GT).field(NodeReferenceTable.COLUMN_S5_GT))
            .subAggregation(AggregationBuilders.sum(NodeReferenceTable.COLUMN_SUMMARY).field(NodeReferenceTable.COLUMN_SUMMARY))
            .subAggregation(AggregationBuilders.sum(NodeReferenceTable.COLUMN_ERROR).field(NodeReferenceTable.COLUMN_ERROR)));

        searchRequestBuilder.addAggregation(aggregationBuilder);
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        JsonArray nodeRefResSumArray = new JsonArray();
        Terms frontApplicationIdTerms = searchResponse.getAggregations().get(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID);
        for (Terms.Bucket frontApplicationIdBucket : frontApplicationIdTerms.getBuckets()) {
            int frontApplicationId = frontApplicationIdBucket.getKeyAsNumber().intValue();
            Terms behindApplicationIdTerms = frontApplicationIdBucket.getAggregations().get(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID);
            for (Terms.Bucket behindApplicationIdBucket : behindApplicationIdTerms.getBuckets()) {
                int behindApplicationId = behindApplicationIdBucket.getKeyAsNumber().intValue();

                if (behindApplicationId != 0) {
                    Sum s1LTE = behindApplicationIdBucket.getAggregations().get(NodeReferenceTable.COLUMN_S1_LTE);
                    Sum s3LTE = behindApplicationIdBucket.getAggregations().get(NodeReferenceTable.COLUMN_S3_LTE);
                    Sum s5LTE = behindApplicationIdBucket.getAggregations().get(NodeReferenceTable.COLUMN_S5_LTE);
                    Sum s5GT = behindApplicationIdBucket.getAggregations().get(NodeReferenceTable.COLUMN_S5_GT);
                    Sum summary = behindApplicationIdBucket.getAggregations().get(NodeReferenceTable.COLUMN_SUMMARY);
                    Sum error = behindApplicationIdBucket.getAggregations().get(NodeReferenceTable.COLUMN_ERROR);
                    logger.debug("frontApplicationId: {}, behindApplicationId: {}, s1LTE: {}, s3LTE: {}, s5LTE: {}, s5GT: {}, error: {}, summary: {}", frontApplicationId,
                        behindApplicationId, s1LTE.getValue(), s3LTE.getValue(), s5LTE.getValue(), s5GT.getValue(), error.getValue(), summary.getValue());

                    JsonObject nodeRefResSumObj = new JsonObject();
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_FRONT_APPLICATION_ID), frontApplicationId);
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_BEHIND_APPLICATION_ID), behindApplicationId);
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_S1_LTE), s1LTE.getValue());
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_S3_LTE), s3LTE.getValue());
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_S5_LTE), s5LTE.getValue());
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_S5_GT), s5GT.getValue());
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_ERROR), error.getValue());
                    nodeRefResSumObj.addProperty(ColumnNameUtils.INSTANCE.rename(NodeReferenceTable.COLUMN_SUMMARY), summary.getValue());
                    nodeRefResSumArray.add(nodeRefResSumObj);
                }
            }
        }

        return nodeRefResSumArray;
    }
}
