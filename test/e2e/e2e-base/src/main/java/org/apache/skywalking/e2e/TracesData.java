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

import org.apache.skywalking.oap.server.core.query.entity.BasicTrace;

import java.util.List;

/**
 * @author kezhenxu94
 */
public class TracesData {
    public static class Traces {
        private List<BasicTrace> data;

        public List<BasicTrace> getData() {
            return data;
        }

        public void setData(final List<BasicTrace> data) {
            this.data = data;
        }
    }

    private Traces traces;

    public Traces getTraces() {
        return traces;
    }

    public void setTraces(final Traces traces) {
        this.traces = traces;
    }

    @Override
    public String toString() {
        return "TracesData{" +
            "traces=" + traces +
            '}';
    }
}
