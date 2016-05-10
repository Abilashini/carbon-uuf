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

import org.wso2.carbon.uuf.internal.util.NameUtils;
import org.wso2.carbon.uuf.spi.Renderable;

public class Layout {

    private final String name;
    private final String simpleName;
    private final Renderable renderer;

    /**
     * @param name     fully qualified name
     * @param renderer renderer
     */
    public Layout(String name, Renderable renderer) {
        this.name = name;
        this.simpleName = NameUtils.getSimpleName(name);
        this.renderer = renderer;
    }

    public String getName() {
        return name;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String render(ComponentLookup lookup, RequestLookup requestLookup) {
        lookup.in(this);
        requestLookup.pushToPublicUriStack(requestLookup.getAppContext() + lookup.getPublicUriInfix(this));
        String output = renderer.render(null, lookup, requestLookup, null);
        requestLookup.popPublicUriStack();
        lookup.out();
        return output;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + (31 * renderer.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (obj instanceof Layout) && (this.name.equals(((Layout) obj).name));
    }

    @Override
    public String toString() {
        return "{\"name\": \"" + name + "\", \"renderer\": " + renderer + "}";
    }
}
