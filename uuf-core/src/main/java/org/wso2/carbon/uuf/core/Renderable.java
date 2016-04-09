package org.wso2.carbon.uuf.core;

import org.wso2.carbon.uuf.model.Model;

public interface Renderable {

    String render(Model model, ComponentLookup componentLookup, RequestLookup requestLookup, API api);
}
