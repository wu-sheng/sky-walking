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

package org.apache.skywalking.apm.agent.core.plugin;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.StringJoiner;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/12/30
 */
public class WitnessMethod {

    /**
     * java.lang.reflect.Method#getDeclaringClass()
     */
    String declaringClassName;
    /**
     * mather fo match the witness method
     */
    ElementMatcher<? super MethodDescription.InDefinedShape> elementMatcher;

    @Override
    public String toString() {
        return new StringJoiner(", ", WitnessMethod.class.getSimpleName() + "[", "]")
                .add("declaringClassName='" + declaringClassName + "'")
                .add("elementMatcher=" + elementMatcher)
                .toString();
    }
}
