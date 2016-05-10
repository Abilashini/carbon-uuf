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

package org.wso2.carbon.uuf.core;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.wso2.carbon.uuf.api.auth.Session;
import org.wso2.carbon.uuf.internal.core.auth.SessionRegistry;
import org.wso2.carbon.uuf.api.auth.User;
import org.wso2.carbon.uuf.exception.HTTPErrorException;
import org.wso2.carbon.uuf.exception.PageRedirectException;
import org.wso2.carbon.uuf.exception.UUFException;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.ws.rs.core.HttpHeaders;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO remove this SuppressWarnings
@SuppressWarnings("PackageAccessibility")
public class API {

    private final SessionRegistry sessionRegistry;
    private final RequestLookup requestLookup;

    public API(SessionRegistry sessionRegistry, RequestLookup requestLookup) {
        this.sessionRegistry = sessionRegistry;
        this.requestLookup = requestLookup;
    }

    /**
     * Returns the result of the method invocation of the best matched OSGi service.
     *
     * @param serviceClassName  service class name
     * @param serviceMethodName method name
     * @param args              method arguments
     * @return
     */
    public static Object callOSGiService(String serviceClassName, String serviceMethodName, Object... args) {
        Object serviceInstance;
        try {
            serviceInstance = (new InitialContext()).lookup("osgi:service/" + serviceClassName);
            if (serviceInstance == null) {
                throw new IllegalArgumentException(
                        "Cannot find any OSGi service registered with the name '" + serviceClassName + "'.");
            }
        } catch (NamingException e) {
            throw new UUFException(
                    "Cannot create the initial context when calling OSGi service '" + serviceClassName + "'.");
        }

        try {
            return MethodUtils.invokeMethod(serviceInstance, serviceMethodName, args);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Cannot find any method with the signature '" + serviceMethodName + "(" + joinClassNames(args) +
                            ")' in OSGi service '" + serviceInstance.getClass().getName() + "' with service class '" +
                            serviceClassName + "'.");
        } catch (Exception e) {
            throw new UUFException(
                    "Invoking method '" + serviceMethodName + "(" + joinClassNames(args) + ")' on OSGi service '" +
                            serviceInstance.getClass().getName() + "' with service class '" + serviceClassName +
                            "' failed.", e);
        }
    }

    /**
     * Returns a map of service implementation class names and instances of all OSGi services for the given service
     * class name.
     *
     * @param serviceClassName service class name
     * @return a map of implementation class and instances
     */
    public static Map<String, Object> getOSGiServices(String serviceClassName) {
        try {
            Context context = new InitialContext();
            NamingEnumeration<Binding> enumeration = context.listBindings("osgi:service/" + serviceClassName);
            Map<String, Object> services = new HashMap<>();
            while (enumeration.hasMore()) {
                Binding binding = enumeration.next();
                services.put(binding.getClassName(), binding.getObject());
            }
            return services;
        } catch (NamingException e) {
            throw new UUFException("Cannot create the initial context when calling OSGi service '" +
                    serviceClassName + "'.");
        }
    }

    public static void callMicroService() {
        // this need to switch network call or osgi call accordingly
        throw new UnsupportedOperationException("To be implemented");
    }

    public static void sendError(int status, String message) {
        throw new HTTPErrorException(status, message);
    }

    public static void sendRedirect(String redirectUrl) {
        throw new PageRedirectException(redirectUrl);
    }

    /**
     * Creates a new session and returns created session.
     *
     * @param userName user name
     * @return {@link Session}
     */
    public Session createSession(String userName) {
        Session session = new Session(new User(userName));
        sessionRegistry.addSession(session);
        Cookie cookie = new DefaultCookie(SessionRegistry.SESSION_COOKIE_NAME, session.getSessionId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        // setting cookie.setSecure(true) will send "Upgrade-Insecure-Requests" -> "1" header
        // for when accessing http instead https
        // cookie.setSecure(true);
        requestLookup.setResponseHeader(HttpHeaders.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
        return session;
    }

    /**
     * Returns the session object. If not found returns {@code null}.
     *
     * @return {@link Session} object
     */
    public Session getSession() {
        String cookieHeader = requestLookup.getRequest().getHeaders().get(HttpHeaders.COOKIE);
        Optional<Cookie> cookie = readCookie(cookieHeader, SessionRegistry.SESSION_COOKIE_NAME);
        if (cookie.isPresent()) {
            Optional<Session> session = sessionRegistry.getSession(cookie.get().value());
            return (session.isPresent()) ? session.get() : null;
        }
        return null;
    }

    /**
     * Sets the theme.
     *
     * @param name theme name
     * @throws IllegalArgumentException when name is null or empty
     */
    public void setTheme(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Cannot set the theme, 'theme-name' can not be empty.");
        }
        Cookie cookie = new DefaultCookie("uuf-theme", name);
        cookie.setHttpOnly(true);
        //TODO: may be we can have multiple themes based on paths
        cookie.setPath("/");
        requestLookup.setResponseHeader(HttpHeaders.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
    }

    /**
     * Returns the theme name. If not found returns {@code null}
     *
     * @return theme name
     */
    public String getTheme() {
        String cookieHeader = requestLookup.getRequest().getHeaders().get(HttpHeaders.COOKIE);
        Optional<Cookie> cookie = readCookie(cookieHeader, "uuf-theme");
        if (cookie.isPresent()) {
            return cookie.get().value();
        }
        return null;
    }

    private Optional<Cookie> readCookie(String header, String name) {
        String[] cookiesParts = header.split(";");
        for (String cookiePart : cookiesParts) {
            if (cookiePart.trim().startsWith(name)) {
                return Optional.of(ClientCookieDecoder.STRICT.decode(cookiePart));
            }
        }
        return Optional.empty();
    }

    private static String joinClassNames(Object[] args) {
        if (args == null) {
            return "null";
        }
        if (args.length == 0) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (Object arg : args) {
            buffer.append(arg.getClass().getName()).append(',');
        }
        return buffer.deleteCharAt(buffer.length() - 1).toString();
    }
}
