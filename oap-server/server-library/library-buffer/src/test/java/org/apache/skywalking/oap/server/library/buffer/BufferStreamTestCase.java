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

package org.apache.skywalking.oap.server.library.buffer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.skywalking.apm.network.language.agent.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class BufferStreamTestCase {

    private static final Logger logger = LoggerFactory.getLogger(BufferStreamTestCase.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String directory = "/Users/pengys5/code/sky-walking/buffer-test";
        BufferStream.Builder<TraceSegmentObject> builder = new BufferStream.Builder<>(directory);
        builder.cleanWhenRestart(true);
        builder.dataFileMaxSize(1);
        builder.offsetFileMaxSize(1);
        builder.parser(TraceSegmentObject.parser());
        builder.callBack(new SegmentParse());

        BufferStream<TraceSegmentObject> stream = builder.build();
        stream.initialize();

        TimeUnit.SECONDS.sleep(5);

        String str = "2018-08-27 11:59:45,261 main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28" +
            "main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28 main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28 main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28" +
            "main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28 main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28 main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28" +
            "main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28 main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28 main DEBUG Registering MBean org.apache.logging.log4j2:type=6d6f6e28";

        for (int i = 0; i < 100; i++) {
            TraceSegmentObject.Builder segment = TraceSegmentObject.newBuilder();
            SpanObject.Builder span = SpanObject.newBuilder();

            span.setOperationName(String.valueOf(i) + "  " + str);
            segment.addSpans(span);
            stream.write(segment.build());
        }

    }

    private static class SegmentParse implements DataStreamReader.CallBack<TraceSegmentObject> {

        @Override public void call(TraceSegmentObject message) {
            logger.info("segment parse: {}", message.getSpans(0).getOperationName());
        }
    }
}