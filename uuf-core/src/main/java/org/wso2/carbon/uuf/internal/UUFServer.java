/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.uuf.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.wso2.carbon.uuf.core.App;
import org.wso2.carbon.uuf.exception.HttpErrorException;
import org.wso2.carbon.uuf.spi.HttpConnector;
import org.wso2.carbon.uuf.spi.HttpRequest;
import org.wso2.carbon.uuf.spi.HttpResponse;
import org.wso2.carbon.uuf.spi.UUFAppRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.wso2.carbon.uuf.spi.HttpResponse.STATUS_BAD_REQUEST;
import static org.wso2.carbon.uuf.spi.HttpResponse.STATUS_INTERNAL_SERVER_ERROR;
import static org.wso2.carbon.uuf.spi.HttpResponse.STATUS_NOT_FOUND;

/**
 * OSGi service component for UUFServer.
 */
@Component(name = "org.wso2.carbon.uuf.internal.UUFServer",
           immediate = true,
           service = UUFAppRegistry.class
)
@SuppressWarnings("unused")
public class UUFServer implements UUFAppRegistry {

    private static final Logger log = LoggerFactory.getLogger(UUFServer.class);

    private final AtomicInteger count = new AtomicInteger(0);
    private final RequestDispatcher requestDispatcher;
    private final Map<String, App> apps;

    public UUFServer() {
        this(new RequestDispatcher());
    }

    public UUFServer(RequestDispatcher requestDispatcher) {
        this.requestDispatcher = requestDispatcher;
        this.apps = new ConcurrentHashMap<>();
    }

    /**
     * This bind method is invoked by OSGi framework whenever a new Connector is registered.
     *
     * @param connector registered connector
     */
    @Reference(name = "httpConnector",
               service = HttpConnector.class,
               cardinality = ReferenceCardinality.AT_LEAST_ONE,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unsetHttpConnector")
    public void setHttpConnector(HttpConnector connector) {
        connector.setServerConnection((request, response) -> {
            MDC.put("uuf-request", String.valueOf(count.incrementAndGet()));
            serve(request, response);
            MDC.remove("uuf-request");
            return response;
        });
        log.info("HttpConnector '" + connector.getClass().getName() + "' registered.");
    }

    /**
     * This bind method is invoked by OSGi framework whenever a Connector is left.
     *
     * @param connector unregistered connector
     */
    public void unsetHttpConnector(HttpConnector connector) {
        connector.setServerConnection(null);
        log.info("HttpConnector '" + connector.getClass().getName() + "' unregistered.");
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        log.debug("UUFServer service activated.");
    }

    @Deactivate
    protected void deactivate(BundleContext bundleContext) {
        log.debug("UUFServer service deactivated.");
    }

    public void serve(HttpRequest request, HttpResponse response) {
        Optional<App> app = Optional.empty();
        try {
            if (!request.isValid()) {
                requestDispatcher.serveErrorPage(request, response, STATUS_BAD_REQUEST,
                                                 "Invalid URI '" + request.getUri() + "'.");
                return;
            }
            if (request.isDefaultFaviconRequest()) {
                requestDispatcher.serveDefaultFavicon(request, response);
                return;
            }

            app = get(request.getContextPath());
            if (!app.isPresent()) {
                requestDispatcher.serveErrorPage(request, response, STATUS_NOT_FOUND,
                                                 "Cannot find an app for context '" + request.getContextPath() + "'.");
                return;
            }
            requestDispatcher.serve(app.get(), request, response);
        } catch (Exception e) {
            log.error("An unexpected error occurred while serving for request '" + request + "'.", e);
            requestDispatcher.serveErrorPage((app.isPresent() ? app.get() : null), request, response,
                                             new HttpErrorException(STATUS_INTERNAL_SERVER_ERROR, e.getMessage(), e));
        }
    }

    @Override
    public App add(App app) {
        return apps.put(app.getContext(), app);
    }

    @Override
    public Optional<App> get(String contextPath) {
        return Optional.ofNullable(apps.get(contextPath));
    }

    @Override
    public Optional<App> remove(String appName) {
        return apps.values().stream()
                .filter(app -> app.getName().equals(appName))
                .findFirst()
                .map(app -> apps.remove(app.getContext()));
    }
}
