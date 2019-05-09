package org.apache.skywalking.oap.server.cluster.plugin.nacos;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.cluster.ClusterModule;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author caoyixiong
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(NamingFactory.class)
@PowerMockIgnore("javax.management.*")
public class ClusterModuleNacosProviderTest {

    private static final String SERVICE_NAME = "test-service_name";

    private ClusterModuleNacosProvider provider = new ClusterModuleNacosProvider();

    @Test
    public void name() {
        assertEquals("nacos", provider.name());
    }

    @Test
    public void module() {
        assertEquals(ClusterModule.class, provider.module());
    }


    @Test
    public void createConfigBeanIfAbsent() {
        ModuleConfig moduleConfig = provider.createConfigBeanIfAbsent();
        assertTrue(moduleConfig instanceof ClusterModuleNacosConfig);
    }

    @Test(expected = ModuleStartException.class)
    public void prepareWithNonHost() throws Exception {
        provider.prepare();
    }

    @Test
    public void prepare() throws Exception {
        PowerMockito.mockStatic(NamingFactory.class);
        ClusterModuleNacosConfig nacosConfig = new ClusterModuleNacosConfig();
        nacosConfig.setHostPort("10.0.0.1:1000,10.0.0.2:1001");
        nacosConfig.setServiceName(SERVICE_NAME);
        Whitebox.setInternalState(provider, "config", nacosConfig);
        NamingService namingService = mock(NamingService.class);
        PowerMockito.when(NamingFactory.createNamingService("10.0.0.1:1000,10.0.0.2:1001")).thenReturn(namingService);
        provider.prepare();
        ArgumentCaptor<String> addressCaptor = ArgumentCaptor.forClass(String.class);
        PowerMockito.verifyStatic();
        NamingFactory.createNamingService(addressCaptor.capture());
        String data = addressCaptor.getValue();
        assertEquals("10.0.0.1:1000,10.0.0.2:1001", data);
    }

    @Test
    public void start() {
        provider.start();
    }

    @Test
    public void notifyAfterCompleted() {
        provider.notifyAfterCompleted();
    }

    @Test
    public void requiredModules() {
        String[] modules = provider.requiredModules();
        assertArrayEquals(new String[]{CoreModule.NAME}, modules);
    }
}
