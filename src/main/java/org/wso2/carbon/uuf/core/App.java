package org.wso2.carbon.uuf.core;

import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private final String context;
    private final List<Page> pages;
    private final Map<String, Renderble> fragmentMap;

    public App(String context, List<Page> pages, Map<String, Renderble> fragmentMap) {
        if (!context.startsWith("/")) {
            throw new IllegalArgumentException("app context must start with a '/'");
        }

        this.context = context;
        this.fragmentMap = fragmentMap;
        // we sort uri so that more wildcard-ed ones go to the bottom.
        Collections.sort(pages, (o1, o2) -> o1.getUri().compareTo(o2.getUri()));
        this.pages = pages;

        //TODO:remove comment
//        // convert the list to maps since we want O(1) access by name.
//        this.fragmentMap = fragmentMap.stream().collect(Collectors.toMap(Fragment::getName, Function.identity()));
    }

    public String serve(HttpRequest request) {
        Page page = getPage(request.getUri());
        if (page != null) {
            if (log.isDebugEnabled()) {
                log.debug("page " + page + " is selected");
            }
            return page.serve(request, fragmentMap);
        } else {
            throw new UUFException(
                    "No page by the URI '" + request.getUri() + "'",
                    Response.Status.NOT_FOUND);
        }
    }

    @Nullable
    public Page getPage(String uri) {
        if (!uri.startsWith(context)) {
            throw new IllegalArgumentException("Request for '" + uri + "' can't be served by " + this);
        }

        Page page = null;
        String relativeUri = uri.substring(context.length());
        for (Page p : pages) {
            if (p.getUri().match(relativeUri)) {
                page = p;
                break;
            }
        }
        return page;
    }

    @Override
    public String toString() {
        return "App{context='" + context + "'}";
    }
}
