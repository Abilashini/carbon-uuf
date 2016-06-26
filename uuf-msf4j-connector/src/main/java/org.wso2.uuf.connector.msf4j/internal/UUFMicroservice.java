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

package org.wso2.uuf.connector.msf4j.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.uuf.api.ServerConnection;
import org.wso2.carbon.uuf.spi.HttpConnector;
import org.wso2.msf4j.HttpStreamer;
import org.wso2.msf4j.Microservice;
import org.wso2.msf4j.Request;
import org.wso2.uuf.connector.msf4j.MicroserviceHttpRequest;
import org.wso2.uuf.connector.msf4j.MicroserviceHttpResponse;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * UUF Connector for MSF4J.
 */
@Component(name = "org.wso2.uuf.connector.msf4j.internal.UUFMicroservice",
           service = {Microservice.class, HttpConnector.class},
           immediate = true)
@Path("/")
public class UUFMicroservice implements Microservice, HttpConnector {

    private static final Logger log = LoggerFactory.getLogger(UUFMicroservice.class);
    private ServerConnection serverConnection;

    @Activate
    protected void activate() {
        log.debug("UUFMicroservice activated.");
    }

    @Deactivate
    protected void deactivate() {
        log.debug("UUFMicroservice deactivated.");
    }

    @GET
    @Path(".*")
    public Response get(@Context Request request) {
        return execute(request, null);
    }

    @POST
    @Path(".*")
    @Produces({"text/plain"})
    public void post(@Context HttpStreamer httpStreamer, @Context Request nettyRequest) {
//        httpStreamer.callback(new HttpStreamHandlerImpl(this, nettyRequest));
    }

    private Response execute(Request request, byte[] contentBytes) {
        MicroserviceHttpRequest httpRequest = new MicroserviceHttpRequest(request, contentBytes);
        MicroserviceHttpResponse httpResponse = new MicroserviceHttpResponse();
        serverConnection.serve(httpRequest, httpResponse);
        return httpResponse.build();
    }

    public void setServerConnection(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }
}
