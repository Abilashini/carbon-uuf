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

package org.wso2.carbon.uuf.handlebars.helpers.runtime;

import com.github.jknack.handlebars.Options;
import org.wso2.carbon.uuf.core.Placeholder;
import org.wso2.carbon.uuf.core.RequestLookup;
import org.wso2.carbon.uuf.handlebars.helpers.FillPlaceholderHelper;

import java.io.IOException;

import static org.wso2.carbon.uuf.handlebars.HbsRenderable.DATA_KEY_REQUEST_LOOKUP;

public class HeadJsHelper extends FillPlaceholderHelper<String> {

    public static final String HELPER_NAME = "headJs";

    public HeadJsHelper() {
        this(Placeholder.HEAD_JS);
    }

    protected HeadJsHelper(Placeholder placeholder) {
        super(placeholder);
    }

    @Override
    public CharSequence apply(String relativePath, Options options) throws IOException {
        RequestLookup requestLookup = options.data(DATA_KEY_REQUEST_LOOKUP);
        StringBuilder buffer = new StringBuilder("<script src=\"")
                .append(requestLookup.getPublicUri())
                .append('/')
                .append(relativePath);
        for (Object param : options.params) {
            buffer.append(param);
        }
        buffer.append("\"");
        // See http://www.w3schools.com/tags/att_script_async.asp
        Object async = options.hash.get("async");
        if ((async != null) && ((Boolean) async)) {
            buffer.append(" async");
        }
        // See http://www.w3schools.com/tags/att_script_defer.asp
        Object defer = options.hash.get("defer");
        if ((defer != null) && ((Boolean) defer)) {
            buffer.append(" defer");
        }
        buffer.append(" type=\"text/javascript\"></script>\n");

        addToPlaceholder(buffer.toString(), options);
        return "";
    }
}
