package org.wso2.carbon.uuf.core;

import org.wso2.carbon.uuf.model.Model;

public class Page implements Comparable<Page> {

    private final UriPatten uriPatten;
    private final Renderable layout;
    private Lookup lookup;

    public Page(UriPatten uriPatten, Renderable layout, Lookup lookup) {
        this.uriPatten = uriPatten;
        this.layout = layout;
        this.lookup = lookup;
    }

    public UriPatten getUriPatten() {
        return uriPatten;
    }

    public String serve(String uri, Model model, Lookup lookup) {
        return layout.render(uri, model, this.lookup);
    }

    @Override
    public String toString() {
        return "{\"uriPattern\": " + uriPatten.toString() + ", \"layout\": " + layout.toString() + "}";
    }

    @Override
    public int compareTo(Page otherPage) {
        return this.getUriPatten().compareTo(otherPage.getUriPatten());
    }
}
