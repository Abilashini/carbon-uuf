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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.wso2.carbon.uuf.api.auth.Session;
import org.wso2.carbon.uuf.api.auth.User;
import org.wso2.carbon.uuf.exception.HttpErrorException;
import org.wso2.carbon.uuf.exception.PageRedirectException;
import org.wso2.carbon.uuf.exception.UUFException;
import org.wso2.carbon.uuf.internal.core.auth.SessionRegistry;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;

// TODO remove this SuppressWarnings
@SuppressWarnings("PackageAccessibility")
public class API {

    private final SessionRegistry sessionRegistry;
    private final RequestLookup requestLookup;
    private Session currentSession;
    private String themeName;

    API(SessionRegistry sessionRegistry, RequestLookup requestLookup) {
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
        // TODO: 5/16/16 Call a Microservice through OSGi or network-call accrodingly
        throw new UnsupportedOperationException("To be implemented");
    }

    public static void sendError(int status, String message) {
        if ((status < 100) || (status > 599)) {
            throw new IllegalArgumentException("HTTP status code must be between 100 (inclusive) and 600 (exclusive).");
        }
        if ((message == null) || message.isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty.");
        }
        throw new HttpErrorException(status, message);
    }

    public static void sendRedirect(String redirectUrl) {
        if ((redirectUrl == null) || redirectUrl.isEmpty()) {
            throw new IllegalArgumentException("Redirect URL cannot be null or empty");
        }
        throw new PageRedirectException(redirectUrl);
    }

    /**
     * Creates a new session and returns created session.
     *
     * @param userName user name
     * @return {@link Session}
     */
    public Session createSession(String userName) {
        // TODO: 5/31/16 if exists, remove current session form SessionRegistry before creating a new one
        Session session = new Session(new User(userName));
        sessionRegistry.addSession(session);
        String header = SessionRegistry.SESSION_COOKIE_NAME + "=" + session.getSessionId() + "; Path=" +
                requestLookup.getAppContext() + "; Secure; HTTPOnly";
        requestLookup.setResponseHeader("Set-Cookie", header);
        return session;
    }

    /**
     * Returns the session object. If not found returns {@code null}.
     *
     * @return {@link Session} object
     */
    public Session getSession() {
        if (currentSession == null) {
            // Since an API object lives in the request scope, it is safe to cache the current Session object.
            String sessionId = requestLookup.getRequest().getCookieValue(SessionRegistry.SESSION_COOKIE_NAME);
            currentSession = StringUtils.isEmpty(sessionId) ? null : sessionRegistry.getSession(sessionId).orElse(null);
        }
        return currentSession;
    }

    public boolean destroySession() {
        Session session = getSession();
        if (session == null) {
            // No session found in the current request.
            return false;
        }

        // Remove session from the SessionRegistry.
        sessionRegistry.removeSession(session.getSessionId());
        // Clear the session cookie by setting its value to an empty string, Max-Age to zero, & Expires to a past date.
        String header = SessionRegistry.SESSION_COOKIE_NAME +
                "=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:01 GMT; Path=" + requestLookup.getAppContext() +
                "; Secure; HTTPOnly";
        requestLookup.setResponseHeader("Set-Cookie", header);
        return true;
    }

    /**
     * Sets the theme.
     *
     * @param themeName theme themeName
     * @throws IllegalArgumentException when themeName is null or empty
     */
    public void setAppTheme(String themeName) {
        if (!Theme.isValidThemeName(themeName)) {
            throw new IllegalArgumentException("Theme name '" + themeName + "' is invalid.");
        }
        this.themeName = themeName;
    }

    /**
     * Returns the theme name. If not found returns {@code null}
     *
     * @return theme name
     */
    public String getAppTheme() {
        return themeName;
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
