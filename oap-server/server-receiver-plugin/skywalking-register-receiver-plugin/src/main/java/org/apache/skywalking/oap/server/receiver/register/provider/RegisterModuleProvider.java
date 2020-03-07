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

package org.apache.skywalking.oap.server.receiver.register.provider;

import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.server.GRPCHandlerRegister;
import org.apache.skywalking.oap.server.core.server.JettyHandlerRegister;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.receiver.register.module.RegisterModule;
import org.apache.skywalking.oap.server.receiver.register.provider.handler.v6.grpc.RegisterServiceHandler;
import org.apache.skywalking.oap.server.receiver.register.provider.handler.v6.grpc.ServiceInstancePingServiceHandler;
import org.apache.skywalking.oap.server.receiver.register.provider.handler.v6.rest.ServiceInstancePingServletHandler;
import org.apache.skywalking.oap.server.receiver.register.provider.handler.v6.rest.ServiceInstanceRegisterServletHandler;
import org.apache.skywalking.oap.server.receiver.register.provider.handler.v6.rest.ServiceRegisterServletHandler;
import org.apache.skywalking.oap.server.receiver.sharing.server.SharingServerModule;

public class RegisterModuleProvider extends ModuleProvider {

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return RegisterModule.class;
    }

    @Override
    public ModuleConfig createConfigBeanIfAbsent() {
        return null;
    }

    @Override
    public void prepare() {
    }

    @Override
    public void start() {
        GRPCHandlerRegister grpcHandlerRegister = getManager().find(SharingServerModule.NAME)
                                                              .provider()
                                                              .getService(GRPCHandlerRegister.class);
        grpcHandlerRegister.addHandler(new RegisterServiceHandler(getManager()));
        grpcHandlerRegister.addHandler(new ServiceInstancePingServiceHandler(getManager()));

        JettyHandlerRegister jettyHandlerRegister = getManager().find(SharingServerModule.NAME)
                                                                .provider()
                                                                .getService(JettyHandlerRegister.class);
        jettyHandlerRegister.addHandler(new ServiceRegisterServletHandler(getManager()));
        jettyHandlerRegister.addHandler(new ServiceInstanceRegisterServletHandler(getManager()));
        jettyHandlerRegister.addHandler(new ServiceInstancePingServletHandler(getManager()));
    }

    @Override
    public void notifyAfterCompleted() {

    }

    @Override
    public String[] requiredModules() {
        return new String[] {
            CoreModule.NAME,
            SharingServerModule.NAME
        };
    }
}
