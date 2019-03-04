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

package org.apache.skywalking.apm.plugin.customize.loader;

import javafx.util.Pair;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.loader.AgentClassLoader;
import org.apache.skywalking.apm.agent.core.plugin.loader.InstrumentationServiceLoader;
import org.apache.skywalking.apm.plugin.customize.conf.CustomizeConfiguration;
import org.apache.skywalking.apm.plugin.customize.define.CustomizeInstanceInstrumentation;
import org.apache.skywalking.apm.plugin.customize.define.CustomizeStaticInstrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The customize instrumentation plugin loader,
 * so implements {@link InstrumentationServiceLoader}
 *
 * @author zhaoyuguang
 */

public class CustomizeInstrumentationLoader implements InstrumentationServiceLoader {

    private static final ILog logger = LogManager.getLogger(CustomizeConfiguration.class);

    @Override
    public List<AbstractClassEnhancePluginDefine> load(AgentClassLoader classLoader) {
        List<AbstractClassEnhancePluginDefine> instrumentations = new ArrayList<AbstractClassEnhancePluginDefine>();
        CustomizeConfiguration.INSTANCE.load();
        Set<Pair<String, Boolean>> enhanceClasses = CustomizeConfiguration.INSTANCE.getInstrumentations();
        try {
            for (Pair<String, Boolean> pair : enhanceClasses) {
                AbstractClassEnhancePluginDefine plugin = (AbstractClassEnhancePluginDefine) Class.forName(
                        pair.getValue() ? CustomizeStaticInstrumentation.class.getName() : CustomizeInstanceInstrumentation.class.getName(),
                        true, classLoader).getConstructor(String.class).newInstance(pair.getKey());
                instrumentations.add(plugin);
            }
        } catch (Exception e) {
            logger.error(e, "InstrumentationServiceLoader loader is error, spi loader is {}", CustomizeInstrumentationLoader.class.getName());
        }
        return instrumentations;
    }
}
