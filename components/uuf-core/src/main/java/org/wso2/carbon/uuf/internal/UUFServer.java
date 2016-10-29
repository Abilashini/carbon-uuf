/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.uuf.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.kernel.startupresolver.RequiredCapabilityListener;
import org.wso2.carbon.uuf.api.Server;
import org.wso2.carbon.uuf.core.App;
import org.wso2.carbon.uuf.exception.UUFException;
import org.wso2.carbon.uuf.internal.core.create.AppDeployer;
import org.wso2.carbon.uuf.internal.io.ArtifactAppDeployer;
import org.wso2.carbon.uuf.spi.HttpConnector;
import org.wso2.carbon.uuf.spi.HttpRequest;
import org.wso2.carbon.uuf.spi.HttpResponse;
import org.wso2.carbon.uuf.spi.RenderableCreator;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.wso2.carbon.uuf.spi.HttpResponse.STATUS_BAD_REQUEST;
import static org.wso2.carbon.uuf.spi.HttpResponse.STATUS_INTERNAL_SERVER_ERROR;
import static org.wso2.carbon.uuf.spi.HttpResponse.STATUS_NOT_FOUND;

@Component(name = "org.wso2.carbon.uuf.internal.UUFServer",
           service = RequiredCapabilityListener.class,
           immediate = true,
           property = {
                   "componentName=wso2-uuf-server"
           }
)
public class UUFServer implements Server, AppDeployer, RequiredCapabilityListener {

    private static final boolean DEV_MODE_ENABLED = Boolean.getBoolean("devmode");
    private static final Logger log = LoggerFactory.getLogger(UUFServer.class);

    private final String appRepositiryPath;
    private final Set<RenderableCreator> renderableCreators;
    private final RequestDispatcher requestDispatcher;
    private ArtifactAppDeployer appDeployer;
    private BundleContext bundleContext;
    private EventPublisher<HttpConnector> eventPublisher;
    private ServiceRegistration appDeployerServiceRegistration;
    private ServiceRegistration serverServiceRegistration;

    public UUFServer() {
        this(Paths.get(System.getProperty("carbon.home", "."), "deployment/uufapps/").toString());
    }

    public UUFServer(String appRepositoryPath) {
        this.appRepositiryPath = appRepositoryPath;
        this.renderableCreators = new HashSet<>();
        this.requestDispatcher = new RequestDispatcher();
    }

    /**
     * This bind method is invoked by OSGi framework whenever a new RenderableCreator is registered.
     *
     * @param renderableCreator registered renderable creator
     */
    @Reference(name = "renderableCreator",
               service = RenderableCreator.class,
               cardinality = ReferenceCardinality.AT_LEAST_ONE,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unsetRenderableCreator")
    public void setRenderableCreator(RenderableCreator renderableCreator) {
        if (!renderableCreators.add(renderableCreator)) {
            throw new IllegalArgumentException(
                    "A RenderableCreator for '" + renderableCreator.getSupportedFileExtensions() +
                            "' extensions is already registered");
        }
        log.info("RenderableCreator '{}' registered for {} extensions.",
                 renderableCreator.getClass().getName(), renderableCreator.getSupportedFileExtensions());
    }

    /**
     * This bind method is invoked by OSGi framework whenever a RenderableCreator is left.
     *
     * @param renderableCreator unregistered renderable creator
     */
    public void unsetRenderableCreator(RenderableCreator renderableCreator) {
        renderableCreators.remove(renderableCreator);
        log.info("RenderableCreator '{}' unregistered for {} extensions.",
                 renderableCreator.getClass().getName(), renderableCreator.getSupportedFileExtensions());
        if (appDeployer != null) {
            /* We have created the 'appDeployer' with the removed RenderableCreator. So we need to create it again
            without the removed RenderableCreator. */
            appDeployer = createArtifactAppDeployer();
        }
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        log.debug("UUF Server activated.");
    }

    @Deactivate
    protected void deactivate(BundleContext bundleContext) {
        renderableCreators.clear();
        appDeployer = null;
        this.bundleContext = null;
        eventPublisher = null;
        appDeployerServiceRegistration.unregister();
        serverServiceRegistration.unregister();
        log.debug("UUF Server deactivated.");
    }

    @Override
    public void onAllRequiredCapabilitiesAvailable() {
        eventPublisher = new EventPublisher<>(bundleContext, HttpConnector.class);
        appDeployer = createArtifactAppDeployer();
        log.debug("ArtifactAppDeployer is ready.");

        appDeployerServiceRegistration = bundleContext.registerService(AppDeployer.class, this, null);
        log.debug("'{}' registered as an AppDeployer.", getClass().getName());

        serverServiceRegistration = bundleContext.registerService(Server.class, this, null);
        log.info("'{}' registered as a Server.", getClass().getName());
    }

    private ArtifactAppDeployer createArtifactAppDeployer() {
        return new ArtifactAppDeployer(appRepositiryPath, renderableCreators);
    }

    @Override
    public void serve(HttpRequest request, HttpResponse response) {
        if (!request.isValid()) {
            requestDispatcher.serveDefaultErrorPage(STATUS_BAD_REQUEST, "Invalid URI '" + request.getUri() + "'.",
                                                    response);
            return;
        }
        if (request.isDefaultFaviconRequest()) {
            requestDispatcher.serveDefaultFavicon(request, response);
            return;
        }

        App app = null;
        try {
            app = appDeployer.getApp(request.getContextPath());
        } catch (UUFException e) {
            String msg = "A server error occurred while serving for request '" + request + "'.";
            log.error(msg, e);
            requestDispatcher.serveDefaultErrorPage(STATUS_INTERNAL_SERVER_ERROR, msg, response);
        } catch (Exception e) {
            String msg = "An unexpected error occurred while serving for request '" + request + "'.";
            log.error(msg, e);
            requestDispatcher.serveDefaultErrorPage(STATUS_INTERNAL_SERVER_ERROR, msg, response);
        }

        if (app == null) {
            requestDispatcher.serveDefaultErrorPage(STATUS_NOT_FOUND, "Cannot find an app for context path '" +
                    request.getContextPath() + "'.", response);
        } else {
            requestDispatcher.serve(app, request, response);
        }
    }

    @Override
    public void deploy() {
        Set<String> deployedAppContexts = appDeployer.deploy();
        for (String deployedAppContext : deployedAppContexts) {
            eventPublisher.publish(httpConnector -> httpConnector.registerAppContextPath(deployedAppContext));
        }
    }

    @Deprecated
    public static boolean isDevModeEnabled() {
        // TODO: 8/13/16 Remove this when Carbon 'Utils.isDevModeEnabled()' is available in C5.20
        return DEV_MODE_ENABLED;
    }
}
