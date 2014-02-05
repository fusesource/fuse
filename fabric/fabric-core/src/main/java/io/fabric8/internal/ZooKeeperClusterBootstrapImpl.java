/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.fabric8.api.scr.Configurer;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration;
import io.fabric8.zookeeper.bootstrap.DataStoreBootstrapTemplate;

import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

import io.fabric8.api.Container;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStoreRegistrationHandler;
import io.fabric8.api.DynamicReference;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.utils.BundleUtils;
import io.fabric8.utils.SystemProperties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

/**
 * ZooKeeperClusterBootstrap
 * |_ ConfigurationAdmin
 * |_ DataStoreRegistrationHandler (@see DataStoreManager)
 * |_ BootstrapConfiguration (@see BootstrapConfiguration)
 * |_ FabricService (optional,unary) (@see FabricServiceImpl)
 *
 */
@ThreadSafe
@Component(name = "io.fabric8.zookeeper.cluster.bootstrap", label = "Fabric8 ZooKeeper Cluster Bootstrap", immediate = true, metatype = false)
@Service(ZooKeeperClusterBootstrap.class)
public final class ZooKeeperClusterBootstrapImpl extends AbstractComponent implements ZooKeeperClusterBootstrap {

    private static final Long FABRIC_SERVICE_TIMEOUT = 60000L;

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = ScrService.class)
    private final ValidatingReference<ScrService> scrService = new ValidatingReference<ScrService>();
    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = DataStoreRegistrationHandler.class)
    private final ValidatingReference<DataStoreRegistrationHandler> registrationHandler = new ValidatingReference<DataStoreRegistrationHandler>();
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    // Public API methods may wait for these services
    @Reference(referenceInterface = FabricService.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private final DynamicReference<FabricService> fabricService = new DynamicReference<FabricService>("Fabric Service", FABRIC_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS);

    @Property(name = "name", label = "Container Name", description = "The name of the container", value = "${karaf.name}")
    private String name;

    @Property(name = "home", label = "Container Home", description = "The home directory of the container", value = "${karaf.home}")
    private String home;

    @Property(name = "data", label = "Container Data", description = "The data directory of the container", value = "${karaf.data}")
    private String data;

    private BundleUtils bundleUtils;

    @Activate
    void activate(ComponentContext componentContext, Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
        this.bundleUtils = new BundleUtils(componentContext.getBundleContext());
        BootstrapConfiguration bootConfig = bootstrapConfiguration.get();
        CreateEnsembleOptions options = bootConfig.getBootstrapOptions();
        if (options.isEnsembleStart()) {
            startBundles(options);
        }

        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public void create(CreateEnsembleOptions options) {
        assertValid();
        try {
            if (options.isClean()) {
                cleanInternal();
            }

            stopBundles();

            BootstrapConfiguration bootConfig = bootstrapConfiguration.get();
            String connectionUrl = bootConfig.getConnectionUrl(options);
            registrationHandler.get().setRegistrationCallback(new DataStoreBootstrapTemplate(name, home, connectionUrl, options));

            bootConfig.createOrUpdateDataStoreConfig(options);
            bootConfig.createZooKeeeperServerConfig(options);
            bootConfig.createZooKeeeperClientConfig(connectionUrl, options);

            startBundles(options);

            if (options.isWaitForProvision() && options.isAgentEnabled()) {
                waitForSuccessfulDeploymentOf(name, options.getProvisionTimeout());
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new FabricException("Unable to create zookeeper server configuration", ex);
        }
	}

    @Override
    public void clean() {
        assertValid();
        cleanInternal();
    }

    private void cleanInternal() {
        try {
            Configuration[] configs = configAdmin.get().listConfigurations("(|(service.factoryPid=io.fabric8.zookeeper.server)(service.pid=io.fabric8.zookeeper))");
            File karafData = new File(data);

            //We are using the ScrService instead of Component context to enable / disable the BootstrapConfiguration.
            //Using the Component context will not deactivate the component and thus cascading will not work, causing multiple issues.
            //So the safest approach here.
            org.apache.felix.scr.Component[] components = scrService.get().getComponents(BootstrapConfiguration.COMPONENT_NAME);
            for (org.apache.felix.scr.Component component : components) {
                component.disable();
            }

            cleanConfigurations(configs);
            cleanZookeeperDirectory(karafData);
            cleanGitDirectory(karafData);

            for (org.apache.felix.scr.Component component : components) {
                component.enable();
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception e) {
            throw new FabricException("Unable to delete zookeeper configuration", e);
        }
    }

    private void cleanConfigurations(Configuration[] configs) throws IOException, InvalidSyntaxException {
        if (configs != null && configs.length > 0) {
            for (Configuration config : configs) {
                config.delete();
            }
        }
    }

    private void cleanZookeeperDirectory(File karafData) throws IOException, InvalidSyntaxException {
        File zkDir = new File(karafData, "zookeeper");
        if (zkDir.isDirectory()) {
            File newZkDir = new File(karafData, "zookeeper." + System.currentTimeMillis());
            if (!zkDir.renameTo(newZkDir)) {
                newZkDir = zkDir;
            }
            delete(newZkDir);
        }
    }

    private void cleanGitDirectory(File karafData) {
        File gitDir = new File(karafData, "git");
        if (gitDir.isDirectory()) {
            delete(gitDir);
        }
    }

    private void waitForSuccessfulDeploymentOf(String containerName, long timeout) throws InterruptedException {
        System.out.println(String.format("Waiting for container %s to provision.", containerName));

        long startedAt = System.currentTimeMillis();
        while (!Thread.interrupted() && startedAt + timeout > System.currentTimeMillis()) {
            try {
                FabricService fabric = fabricService.getIfPresent();
                Container container = fabric != null ? fabric.getContainer(containerName) : null;
                if (container != null && container.isAlive() && "success".equals(container.getProvisionStatus())) {
                    return;
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                throw FabricException.launderThrowable(t);
            }
        }
    }

    private void stopBundles() throws BundleException {
        bundleUtils.findAndStopBundle("io.fabric8.fabric-agent");
    }

    private void startBundles(CreateEnsembleOptions options) throws BundleException {
        Bundle agentBundle = bundleUtils.findBundle("io.fabric8.fabric-agent");
        if (agentBundle != null && options.isAgentEnabled()) {
            agentBundle.start();
        }
    }

    private static void delete(File dir) {
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                delete(child);
            }
        }
        if (dir.exists()) {
            dir.delete();
        }
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindScrService(ScrService service) {
        this.scrService.bind(service);
    }

    void unbindScrService(ScrService service) {
        this.scrService.unbind(service);
    }

    void bindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.bind(service);
    }

    void unbindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.unbind(service);
    }

    void bindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.bind(service);
    }

    void unbindRegistrationHandler(DataStoreRegistrationHandler service) {
        this.registrationHandler.unbind(service);
    }

    void bindFabricService(FabricService service) {
        this.fabricService.bind(service);
    }

    void unbindFabricService(FabricService service) {
        this.fabricService.unbind(service);
    }
}
