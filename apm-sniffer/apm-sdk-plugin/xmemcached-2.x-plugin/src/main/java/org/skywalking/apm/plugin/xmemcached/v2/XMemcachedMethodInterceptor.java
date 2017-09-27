package org.skywalking.apm.plugin.xmemcached.v2;

import java.lang.reflect.Method;

import org.skywalking.apm.agent.core.context.ContextManager;
import org.skywalking.apm.agent.core.context.tag.Tags;
import org.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.skywalking.apm.logging.ILog;
import org.skywalking.apm.logging.LogManager;
import org.skywalking.apm.network.trace.component.ComponentsDefine;

import net.rubyeye.xmemcached.command.Command;

public class XMemcachedMethodInterceptor implements InstanceMethodsAroundInterceptor {

	 private static final ILog logger = LogManager.getLogger(XMemcachedMethodInterceptor.class);
	 
    @Override public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        
    	Object[] arguments = allArguments;
    	String peer = String.valueOf(objInst.getSkyWalkingDynamicField());
        AbstractSpan span = ContextManager.createExitSpan("XMemcached/" + method.getName(), peer);
        span.setComponent(ComponentsDefine.MEMCACHE);
        Tags.DB_TYPE.set(span, "Memcache");
        SpanLayer.asDB(span);

        if (allArguments.length > 0 && arguments[0] instanceof String) {
        	// Command command =  (Command)allArguments[0];
            // Tags.DB_STATEMENT.set(span, command.getCommandType().name() + " " + command.getKey());
        	Tags.DB_STATEMENT.set(span, method.getName() + " " + arguments[0]);
        }
    }

    @Override public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(t);
    }
}
