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

package org.wso2.carbon.uuf.renderablecreator.hbs.helpers.runtime;

import com.github.jknack.handlebars.Options;
import org.wso2.carbon.uuf.api.Placeholder;
import org.wso2.carbon.uuf.renderablecreator.hbs.helpers.FillPlaceholderHelper;

import java.io.IOException;
import java.util.Optional;

public class TitleHelper extends FillPlaceholderHelper<String> {

    public static final String HELPER_NAME = "title";

    public TitleHelper() {
        super(Placeholder.title);
    }

    public CharSequence apply(String title, Options options) throws IOException {
        if (title == null) {
            throw new IllegalArgumentException("Title cannot be null.");
        }

        Optional<String> currentTitle = getPlaceholderValue(options);
        if (currentTitle.isPresent()) {
            throw new IllegalStateException(
                    "Cannot set page title. It is already set to '" + currentTitle.get() + "'.");
        } else {
            StringBuilder buffer = new StringBuilder(title);
            for (Object param : options.params) {
                buffer.append(param);
            }
            addToPlaceholder(buffer.toString(), options);
            return "";
        }
    }
}
