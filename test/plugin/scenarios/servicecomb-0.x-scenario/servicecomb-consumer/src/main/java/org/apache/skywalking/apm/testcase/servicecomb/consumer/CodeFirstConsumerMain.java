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

package org.apache.skywalking.apm.testcase.servicecomb.consumer;

import io.servicecomb.foundation.common.utils.BeanUtils;
import io.servicecomb.foundation.common.utils.Log4jUtils;
import org.springframework.stereotype.Component;

@Component
public class CodeFirstConsumerMain {

    public static void main(String[] args) {
        init();
    }

    public static void init() {
        while (true) {
            try {
                Log4jUtils.init();
                BeanUtils.init();
                return;
            } catch (Throwable e) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }
    }

}
