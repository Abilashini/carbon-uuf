package org.wso2.carbon.uuf.core;

import org.wso2.carbon.uuf.model.Model;

public class Page implements Comparable<Page> {

    private final UriPatten uriPatten;
    private final Renderable renderer;

    public Page(UriPatten uriPatten, Renderable renderer) {
        this.uriPatten = uriPatten;
        this.renderer = renderer;
    }

    public UriPatten getUriPatten() {
        return uriPatten;
    }

    public String render(Model model, ComponentLookup componentLookup, RequestLookup requestLookup, API api) {
        requestLookup.pushToPublicUriStack(requestLookup.getAppContext() + componentLookup.getPublicUriInfix(this));
        String output = renderer.render(model, componentLookup, requestLookup, api);
        requestLookup.popPublicUriStack();
        return output;
    }

    @Override
    public String toString() {
        return "{\"uriPattern\": \"" + uriPatten.toString() + "\", \"renderer\": \"" + renderer + "\"}";
    }

    @Override
    public int compareTo(Page otherPage) {
        return this.getUriPatten().compareTo(otherPage.getUriPatten());
    }
}
