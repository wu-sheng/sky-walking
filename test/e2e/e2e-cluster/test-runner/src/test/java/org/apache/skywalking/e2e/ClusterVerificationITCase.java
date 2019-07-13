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

package org.apache.skywalking.e2e;

import org.apache.skywalking.e2e.metrics.AtLeastOneOfMetricsMatcher;
import org.apache.skywalking.e2e.metrics.Metrics;
import org.apache.skywalking.e2e.metrics.MetricsQuery;
import org.apache.skywalking.e2e.metrics.MetricsValueMatcher;
import org.apache.skywalking.e2e.service.Service;
import org.apache.skywalking.e2e.service.ServicesMatcher;
import org.apache.skywalking.e2e.service.ServicesQuery;
import org.apache.skywalking.e2e.service.endpoint.Endpoint;
import org.apache.skywalking.e2e.service.endpoint.EndpointQuery;
import org.apache.skywalking.e2e.service.endpoint.Endpoints;
import org.apache.skywalking.e2e.service.endpoint.EndpointsMatcher;
import org.apache.skywalking.e2e.service.instance.Instance;
import org.apache.skywalking.e2e.service.instance.Instances;
import org.apache.skywalking.e2e.service.instance.InstancesMatcher;
import org.apache.skywalking.e2e.service.instance.InstancesQuery;
import org.apache.skywalking.e2e.topo.TopoData;
import org.apache.skywalking.e2e.topo.TopoMatcher;
import org.apache.skywalking.e2e.topo.TopoQuery;
import org.apache.skywalking.e2e.trace.Trace;
import org.apache.skywalking.e2e.trace.TracesMatcher;
import org.apache.skywalking.e2e.trace.TracesQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.skywalking.e2e.metrics.MetricsQuery.ALL_ENDPOINT_METRICS;
import static org.apache.skywalking.e2e.metrics.MetricsQuery.ALL_INSTANCE_METRICS;
import static org.apache.skywalking.e2e.metrics.MetricsQuery.ALL_SERVICE_METRICS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kezhenxu94
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ClusterVerificationITCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterVerificationITCase.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private SimpleQueryClient queryClient;
    private String instrumentedServiceUrl;

    @Before
    public void setUp() {
        final String swWebappHost = System.getProperty("sw.webapp.host", "127.0.0.1");
        final String swWebappPort = System.getProperty("sw.webapp.port", "32783");
        final String instrumentedServiceHost = System.getProperty("service.host", "127.0.0.1");
        final String instrumentedServicePort = System.getProperty("service.port", "32782");
        queryClient = new SimpleQueryClient(swWebappHost, swWebappPort);
        instrumentedServiceUrl = "http://" + instrumentedServiceHost + ":" + instrumentedServicePort;
    }

    @Test(timeout = 600000)
    @DirtiesContext
    public void verify() throws Exception {
        // warm up to ensure the service is registered

        LocalDateTime startTime = LocalDateTime.now(ZoneOffset.UTC);

        final Map<String, String> user = new HashMap<>();
        user.put("name", "SkyWalking");
        List<Trace> traces = Collections.emptyList();
        while (traces.isEmpty()) {
            try {
                restTemplate.postForEntity(
                    instrumentedServiceUrl + "/e2e/users",
                    user,
                    String.class
                );
                Thread.sleep(10000);
                traces = queryClient.traces(
                    new TracesQuery()
                        .start(startTime)
                        .end(LocalDateTime.now(ZoneOffset.UTC))
                        .orderByStartTime()
                );
            } catch (Throwable ignored) {
            }
        }

        final ResponseEntity<String> responseEntity = restTemplate.postForEntity(
            instrumentedServiceUrl + "/e2e/users",
            user,
            String.class
        );
        LOGGER.info("responseEntity: {}, {}", responseEntity.getStatusCode(), responseEntity.getBody());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        Thread.sleep(30000);

        verifyTraces(startTime);

        verifyServices(startTime);

        verifyTopo(startTime);
    }

    private void verifyTopo(LocalDateTime minutesAgo) throws Exception {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        final TopoData topoData = queryClient.topo(
            new TopoQuery()
                .stepByMinute()
                .start(minutesAgo.minusDays(1))
                .end(now)
        );

        InputStream expectedInputStream =
            new ClassPathResource("expected-data/org.apache.skywalking.e2e.ClusterVerificationITCase.topo.yml").getInputStream();

        final TopoMatcher topoMatcher = new Yaml().loadAs(expectedInputStream, TopoMatcher.class);
        topoMatcher.verify(topoData);
    }

    private void verifyServices(LocalDateTime minutesAgo) throws Exception {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        final List<Service> services = queryClient.services(
            new ServicesQuery()
                .start(minutesAgo)
                .end(now)
        );

        InputStream expectedInputStream =
            new ClassPathResource("expected-data/org.apache.skywalking.e2e.ClusterVerificationITCase.services.yml").getInputStream();

        final ServicesMatcher servicesMatcher = new Yaml().loadAs(expectedInputStream, ServicesMatcher.class);
        servicesMatcher.verify(services);

        for (Service service : services) {
            LOGGER.info("verifying service instances: {}", service);

            verifyServiceMetrics(service, minutesAgo);

            Instances instances = verifyServiceInstances(minutesAgo, now, service);

            verifyInstancesMetrics(instances, minutesAgo);

            Endpoints endpoints = verifyServiceEndpoints(minutesAgo, now, service);

            verifyEndpointsMetrics(endpoints, minutesAgo);
        }
    }

    private Instances verifyServiceInstances(LocalDateTime minutesAgo, LocalDateTime now, Service service) throws Exception {
        InputStream expectedInputStream;
        Instances instances = queryClient.instances(
            new InstancesQuery()
                .serviceId(service.getKey())
                .start(minutesAgo)
                .end(now)
        );
        expectedInputStream =
            new ClassPathResource("expected-data/org.apache.skywalking.e2e.ClusterVerificationITCase.instances.yml").getInputStream();
        final InstancesMatcher instancesMatcher = new Yaml().loadAs(expectedInputStream, InstancesMatcher.class);
        instancesMatcher.verify(instances);
        return instances;
    }

    private Endpoints verifyServiceEndpoints(LocalDateTime minutesAgo, LocalDateTime now, Service service) throws Exception {
        Endpoints instances = queryClient.endpoints(
            new EndpointQuery().serviceId(service.getKey())
        );
        InputStream expectedInputStream =
            new ClassPathResource("expected-data/org.apache.skywalking.e2e.ClusterVerificationITCase.endpoints.yml").getInputStream();
        final EndpointsMatcher endpointsMatcher = new Yaml().loadAs(expectedInputStream, EndpointsMatcher.class);
        endpointsMatcher.verify(instances);
        return instances;
    }

    private void verifyInstancesMetrics(Instances instances, final LocalDateTime minutesAgo) throws Exception {
        for (Instance instance : instances.getInstances()) {
            for (String metricsName : ALL_INSTANCE_METRICS) {
                LOGGER.info("verifying service instance response time: {}", instance);
                final Metrics instanceRespTime = queryClient.metrics(
                    new MetricsQuery()
                        .stepByMinute()
                        .metricsName(metricsName)
                        .id(instance.getKey())
                );
                AtLeastOneOfMetricsMatcher instanceRespTimeMatcher = new AtLeastOneOfMetricsMatcher();
                MetricsValueMatcher greaterThanZero = new MetricsValueMatcher();
                greaterThanZero.setValue("gt 0");
                instanceRespTimeMatcher.setValue(greaterThanZero);
                instanceRespTimeMatcher.verify(instanceRespTime);
                LOGGER.info("{}: {}", metricsName, instanceRespTime);
            }
        }
    }

    private void verifyEndpointsMetrics(Endpoints endpoints, final LocalDateTime minutesAgo) throws Exception {
        for (Endpoint endpoint : endpoints.getEndpoints()) {
            if (!endpoint.getLabel().equals("/e2e/users")) {
                continue;
            }
            for (String metricName : ALL_ENDPOINT_METRICS) {
                LOGGER.info("verifying endpoint {}, metrics: {}", endpoint, metricName);
                final Metrics metrics = queryClient.metrics(
                    new MetricsQuery()
                        .stepByMinute()
                        .metricsName(metricName)
                        .start(minutesAgo)
                        .end(LocalDateTime.now(ZoneOffset.UTC))
                        .id(endpoint.getKey())
                );
                AtLeastOneOfMetricsMatcher instanceRespTimeMatcher = new AtLeastOneOfMetricsMatcher();
                MetricsValueMatcher greaterThanZero = new MetricsValueMatcher();
                greaterThanZero.setValue("gt 0");
                instanceRespTimeMatcher.setValue(greaterThanZero);
                instanceRespTimeMatcher.verify(metrics);
                LOGGER.info("metrics: {}", metrics);
            }
        }
    }

    private void verifyServiceMetrics(Service service, final LocalDateTime minutesAgo) throws Exception {
        for (String metricName : ALL_SERVICE_METRICS) {
            LOGGER.info("verifying service {}, metrics: {}", service, metricName);
            final Metrics instanceRespTime = queryClient.metrics(
                new MetricsQuery()
                    .stepByMinute()
                    .metricsName(metricName)
                    .start(minutesAgo)
                    .end(LocalDateTime.now(ZoneOffset.UTC))
                    .id(service.getKey())
            );
            AtLeastOneOfMetricsMatcher instanceRespTimeMatcher = new AtLeastOneOfMetricsMatcher();
            MetricsValueMatcher greaterThanZero = new MetricsValueMatcher();
            greaterThanZero.setValue("gt 0");
            instanceRespTimeMatcher.setValue(greaterThanZero);
            instanceRespTimeMatcher.verify(instanceRespTime);
            LOGGER.info("instanceRespTime: {}", instanceRespTime);
        }
    }

    private void verifyTraces(LocalDateTime minutesAgo) throws Exception {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        final List<Trace> traces = queryClient.traces(
            new TracesQuery()
                .start(minutesAgo)
                .end(now)
                .orderByStartTime()
        );

        InputStream expectedInputStream =
            new ClassPathResource("expected-data/org.apache.skywalking.e2e.ClusterVerificationITCase.traces.yml").getInputStream();

        final TracesMatcher tracesMatcher = new Yaml().loadAs(expectedInputStream, TracesMatcher.class);
        tracesMatcher.verifyLoosely(traces);
    }
}
