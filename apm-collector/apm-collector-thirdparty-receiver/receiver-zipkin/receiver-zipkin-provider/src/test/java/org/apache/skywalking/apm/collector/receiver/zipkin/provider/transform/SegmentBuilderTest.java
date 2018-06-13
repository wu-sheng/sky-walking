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

package org.apache.skywalking.apm.collector.receiver.zipkin.provider.transform;

import org.apache.skywalking.apm.collector.analysis.register.define.service.AgentOsInfo;
import org.apache.skywalking.apm.collector.analysis.register.define.service.IApplicationIDService;
import org.apache.skywalking.apm.collector.analysis.register.define.service.IInstanceIDService;
import org.apache.skywalking.apm.collector.receiver.zipkin.provider.RegisterServices;
import org.apache.skywalking.apm.collector.receiver.zipkin.provider.data.ZipkinTrace;
import org.apache.skywalking.apm.network.proto.TraceSegmentObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author wusheng
 */
public class SegmentBuilderTest implements SegmentListener {
    private Map<String, Integer> applicationInstRegister = new HashMap<>();
    private Map<String, Integer> applicationRegister = new HashMap<>();
    private int appIdSeg = 1;
    private int appInstIdSeq = 1;

    @Test
    public void testTransform() throws UnsupportedEncodingException {

        IApplicationIDService applicationIDService = new IApplicationIDService() {
            @Override
            public int getOrCreateForApplicationCode(String applicationCode) {
                String key = "AppCode:" + applicationCode;
                if (applicationRegister.containsKey(key)) {
                    return applicationRegister.get(key);
                } else {
                    int id = appIdSeg++;
                    applicationRegister.put(key, id);
                    return id;
                }
            }

            @Override
            public int getOrCreateForAddressId(int addressId, String networkAddress) {
                String key = "Address:" + networkAddress;
                if (applicationRegister.containsKey(key)) {
                    return applicationRegister.get(key);
                } else {
                    int id = appIdSeg++;
                    applicationRegister.put(key, id);
                    return id;
                }
            }
        };

        IInstanceIDService instanceIDService = new IInstanceIDService() {
            @Override
            public int getOrCreateByAgentUUID(int applicationId, String agentUUID, long registerTime, AgentOsInfo osInfo) {
                String key = "AppCode:" + applicationId + ",UUID:" + agentUUID;
                if (applicationInstRegister.containsKey(key)) {
                    return applicationInstRegister.get(key);
                } else {
                    int id = appInstIdSeq++;
                    applicationInstRegister.put(key, id);
                    return id;
                }
            }

            @Override
            public int getOrCreateByAddressId(int applicationId, int addressId, long registerTime) {
                String key = "VitualAppCode:" + applicationId + ",address:" + addressId;
                if (applicationInstRegister.containsKey(key)) {
                    return applicationInstRegister.get(key);
                } else {
                    int id = appInstIdSeq++;
                    applicationInstRegister.put(key, id);
                    return id;
                }
            }
        };
        RegisterServices services = new RegisterServices(applicationIDService, instanceIDService, null, null);

        Zipkin2SkyWalkingTransfer.INSTANCE.addListener(this);
        Zipkin2SkyWalkingTransfer.INSTANCE.setRegisterServices(services);

        List<Span> spanList = buildSpringSleuthExampleTrace();
        Assert.assertEquals(3, spanList.size());

        ZipkinTrace trace = new ZipkinTrace();
        spanList.forEach(span -> trace.addSpan(span));

        Zipkin2SkyWalkingTransfer.INSTANCE.transfer(trace);
    }

    private List<Span> buildSpringSleuthExampleTrace() throws UnsupportedEncodingException {
        List<Span> spans = new LinkedList<>();
        String span = "{\"traceId\":\"5b0e64354eea4fa71a8a1b5bdd791b8a\",\"id\":\"1a8a1b5bdd791b8a\",\"kind\":\"SERVER\",\"name\":\"get /\",\"timestamp\":1527669813700123,\"duration\":11295,\"localEndpoint\":{\"serviceName\":\"frontend\",\"ipv4\":\"192.168.72.220\"},\"remoteEndpoint\":{\"ipv6\":\"::1\",\"port\":55146},\"tags\":{\"http.method\":\"GET\",\"http.path\":\"/\",\"mvc.controller.class\":\"Frontend\",\"mvc.controller.method\":\"callBackend\"}}";
        spans.add(SpanBytesDecoder.JSON_V2.decodeOne(span.getBytes("UTF-8")));
        span = "{\"traceId\":\"5b0e64354eea4fa71a8a1b5bdd791b8a\",\"parentId\":\"1a8a1b5bdd791b8a\",\"id\":\"d7d5b93dcda767c8\",\"kind\":\"CLIENT\",\"name\":\"get\",\"timestamp\":1527669813702456,\"duration\":6672,\"localEndpoint\":{\"serviceName\":\"frontend\",\"ipv4\":\"192.168.72.220\"},\"tags\":{\"http.method\":\"GET\",\"http.path\":\"/api\"}}";
        spans.add(SpanBytesDecoder.JSON_V2.decodeOne(span.getBytes("UTF-8")));
        span = "{\"traceId\":\"5b0e64354eea4fa71a8a1b5bdd791b8a\",\"parentId\":\"1a8a1b5bdd791b8a\",\"id\":\"d7d5b93dcda767c8\",\"kind\":\"SERVER\",\"name\":\"get /api\",\"timestamp\":1527669813705106,\"duration\":4802,\"localEndpoint\":{\"serviceName\":\"backend\",\"ipv4\":\"192.168.72.220\"},\"remoteEndpoint\":{\"ipv4\":\"127.0.0.1\",\"port\":55147},\"tags\":{\"http.method\":\"GET\",\"http.path\":\"/api\",\"mvc.controller.class\":\"Backend\",\"mvc.controller.method\":\"printDate\"},\"shared\":true}";
        spans.add(SpanBytesDecoder.JSON_V2.decodeOne(span.getBytes("UTF-8")));

        return SpanBytesDecoder.JSON_V2.decodeList(spans.toString().getBytes("UTF-8"));
    }

    @Override
    public void notify(List<TraceSegmentObject.Builder> segments) {
        Assert.assertEquals(2, segments.size());
    }
}
