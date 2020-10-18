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

package org.apache.skywalking.apm.agent.core.context;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * Extension context, It provides the interaction capabilities between the agents deployed in upstream and downstream
 * services.
 */
public class ExtensionContext {

    private static final ILog LOGGER = LogManager.getLogger(ExtensionContext.class);
    /**
     * Tracing Mode. If true means represents all spans generated in this context should skip analysis.
     */
    private boolean skipAnalysis;

    /**
     * The sending timestamp of the exit span.
     */
    @Getter
    @Setter
    private long sendingTimestamp;

    /**
     * Serialize this {@link ExtensionContext} to a {@link String}
     *
     * @return the serialization string.
     */
    String serialize() {
        String res = (skipAnalysis ? "1" : "0");
        res += sendingTimestamp == 0 ? "-" : ("-" + sendingTimestamp);
        return res;
    }

    /**
     * Deserialize data from {@link String}
     */
    void deserialize(String value) {
        if (StringUtil.isEmpty(value)) {
            return;
        }
        String[] extensionParts = value.replaceAll("-", "- ").split("-");
        // All parts of the extension header are optional.
        // only try to read it when it exist.
        if (extensionParts.length == 2) {
            String extensionPart = extensionParts[0].trim();
            this.skipAnalysis = Objects.equals(extensionPart, "1");
            extensionPart = extensionParts[1].trim();
            if (StringUtil.isNotEmpty(extensionPart)) {
                try {
                    this.sendingTimestamp = Long.parseLong(extensionPart);
                } catch (NumberFormatException e) {
                    LOGGER.error(e, "the downstream sending timestamp is illegal:[{}]", extensionParts[1]);
                }
            }
        }
    }

    /**
     * Prepare for the cross-process propagation.
     */
    void inject(ContextCarrier carrier) {
        carrier.getExtensionContext().skipAnalysis = this.skipAnalysis;
    }

    /**
     * Extra the {@link ContextCarrier#getExtensionContext()} into this context.
     */
    void extract(ContextCarrier carrier) {
        this.skipAnalysis = carrier.getExtensionContext().skipAnalysis;
    }

    /**
     * Handle the tracing span.
     */
    void handle(AbstractSpan span) {
        if (this.skipAnalysis) {
            span.skipAnalysis();
        }
        if (this.sendingTimestamp != 0) {
            Tags.TRANSMISSION_LATENCY.set(span, String.valueOf(System.currentTimeMillis() - sendingTimestamp));
        }
    }

    /**
     * Clone the context data, work for capture to cross-thread.
     */
    @Override
    public ExtensionContext clone() {
        final ExtensionContext context = new ExtensionContext();
        context.skipAnalysis = this.skipAnalysis;
        context.sendingTimestamp = this.sendingTimestamp;
        return context;
    }

    void continued(ContextSnapshot snapshot) {
        this.skipAnalysis = snapshot.getExtensionContext().skipAnalysis;
        this.sendingTimestamp = snapshot.getExtensionContext().sendingTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExtensionContext that = (ExtensionContext) o;
        return skipAnalysis == that.skipAnalysis && sendingTimestamp == that.sendingTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipAnalysis, sendingTimestamp);
    }
}
