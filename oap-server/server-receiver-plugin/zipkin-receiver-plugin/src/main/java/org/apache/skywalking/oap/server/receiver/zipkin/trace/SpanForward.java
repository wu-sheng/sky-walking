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

package org.apache.skywalking.oap.server.receiver.zipkin.trace;

import java.util.List;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.cache.*;
import org.apache.skywalking.oap.server.core.source.*;
import org.apache.skywalking.oap.server.library.util.BooleanUtils;
import org.apache.skywalking.oap.server.receiver.zipkin.*;
import org.apache.skywalking.oap.server.storage.plugin.zipkin.ZipkinSpan;
import zipkin2.Span;

/**
 * @author wusheng
 */
public class SpanForward {
    private ZipkinReceiverConfig config;
    private SourceReceiver receiver;
    private ServiceInventoryCache serviceInventoryCache;
    private EndpointInventoryCache endpointInventoryCache;

    public SpanForward(ZipkinReceiverConfig config, SourceReceiver receiver,
        ServiceInventoryCache serviceInventoryCache,
        EndpointInventoryCache endpointInventoryCache) {
        this.config = config;
        this.receiver = receiver;
        this.serviceInventoryCache = serviceInventoryCache;
        this.endpointInventoryCache = endpointInventoryCache;
    }

    public void send(List<Span> spanList) {
        spanList.forEach(span -> {
            ZipkinSpan zipkinSpan = new ZipkinSpan();
            zipkinSpan.setTraceId(span.traceId());
            zipkinSpan.setSpanId(span.id());
            String serviceName = span.localServiceName();
            int serviceId = Const.NONE;
            if (!StringUtil.isEmpty(serviceName)) {
                serviceId = serviceInventoryCache.getServiceId(serviceName);
                if (serviceId != Const.NONE) {
                    zipkinSpan.setServiceId(serviceId);
                } else {
                    /**
                     * Only register, but don't wait.
                     * For this span, service id will be missed.
                     */
                    CoreRegisterLinker.getServiceInventoryRegister().getOrCreate(serviceName, null);
                }
            }

            String spanName = span.name();
            Span.Kind kind = span.kind();
            switch (kind) {
                case SERVER:
                case CONSUMER:
                    if (!StringUtil.isEmpty(spanName) && serviceId != Const.NONE) {
                        int endpointId = endpointInventoryCache.getEndpointId(serviceId, spanName,
                            DetectPoint.SERVER.ordinal());
                        if (endpointId != Const.NONE) {
                            zipkinSpan.setEndpointId(endpointId);
                        } else if (config.isRegisterZipkinEndpoint()) {
                            CoreRegisterLinker.getEndpointInventoryRegister().getOrCreate(serviceId, spanName, DetectPoint.SERVER);
                        }
                    }
            }
            if (!StringUtil.isEmpty(spanName)) {
                zipkinSpan.setEndpointName(spanName);
            }

            zipkinSpan.setStartTime(span.timestampAsLong());
            zipkinSpan.setEndTime(span.timestampAsLong() + span.durationAsLong());
            zipkinSpan.setIsError(BooleanUtils.booleanToValue(false));

            receiver.receive(zipkinSpan);
        });
    }
}
