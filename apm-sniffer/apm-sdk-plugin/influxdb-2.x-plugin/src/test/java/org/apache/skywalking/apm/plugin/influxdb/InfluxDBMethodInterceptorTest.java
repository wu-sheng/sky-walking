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

package org.apache.skywalking.apm.plugin.influxdb;

import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.LogDataEntity;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.influxdb.interceptor.InfluxDBMethodInterceptor;
import org.hamcrest.CoreMatchers;
import org.influxdb.InfluxDBException;
import org.influxdb.dto.Point;
import org.influxdb.impl.InfluxDBImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class InfluxDBMethodInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    @Mock
    private EnhancedInstance enhancedInstance;

    private InfluxDBMethodInterceptor interceptor;

    private Object[] allArgument;

    private Class[] argumentType;

    @Before
    public void setUp() throws Exception {
        allArgument = new Object[] {
            Point.measurement("cpu")
                .tag("host", "127.0.0.1")
                .addField("use_idle", 0.8)
                .build()
        };
        argumentType = new Class[] {
            Point.class
        };

        interceptor = new InfluxDBMethodInterceptor();
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn("http://127.0.0.1:8086");
    }

    @Test
    public void testIntercept() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getMockWriteMethod(), allArgument, argumentType, null);
        interceptor.afterMethod(enhancedInstance, getMockWriteMethod(), allArgument, argumentType, null);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertInfluxDBSpan(spans.get(0));
    }

    @Test
    public void testInterceptWithException() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getMockWriteMethod(), allArgument, argumentType, null);
        interceptor.handleMethodException(enhancedInstance, getMockWriteMethod(), allArgument, argumentType, new InfluxDBException("test exception"));
        interceptor.afterMethod(enhancedInstance, getMockWriteMethod(), allArgument, argumentType, null);

        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));
        assertInfluxDBSpan(spans.get(0));

        assertLogData(SpanHelper.getLogs(spans.get(0)));
    }

    private void assertLogData(List<LogDataEntity> logDataEntities) {
        assertThat(logDataEntities.size(), is(1));
        LogDataEntity logData = logDataEntities.get(0);
        Assert.assertThat(logData.getLogs().size(), is(4));
        Assert.assertThat(logData.getLogs().get(0).getValue(), CoreMatchers.<Object>is("error"));
        Assert.assertThat(logData.getLogs()
                                 .get(1)
                                 .getValue(), CoreMatchers.<Object>is(InfluxDBException.class.getName()));
        Assert.assertEquals("test exception", logData.getLogs().get(2).getValue());
        assertNotNull(logData.getLogs().get(3).getValue());
    }

    private void assertInfluxDBSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), is("InfluxDB/write"));
        assertThat(span.isExit(), is(true));
        assertThat(SpanHelper.getComponentId(span), is(ComponentsDefine.INFLUXDB_JAVA.getId()));
        List<TagValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("InfluxDB"));
        assertThat(tags.get(1).getValue(), is("write ".concat(allArgument[0].toString())));
        assertThat(SpanHelper.getLayer(span), CoreMatchers.is(SpanLayer.DB));
    }

    private Method getMockWriteMethod() {
        try {
            return InfluxDBImpl.class.getMethod("write", Point.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

}
