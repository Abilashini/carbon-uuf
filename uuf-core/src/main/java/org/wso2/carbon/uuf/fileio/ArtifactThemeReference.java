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

package org.wso2.carbon.uuf.fileio;

import org.wso2.carbon.uuf.core.create.FileReference;
import org.wso2.carbon.uuf.core.create.ThemeReference;
import org.wso2.carbon.uuf.core.exception.UUFException;

import java.nio.file.Files;
import java.nio.file.Path;

public class ArtifactThemeReference implements ThemeReference {

    private final Path path;
    private final ArtifactAppReference appReference;

    public ArtifactThemeReference(Path path, ArtifactAppReference appReference) {
        this.path = path;
        this.appReference = appReference;
    }

    @Override
    public String getName() {
        Path fileName = path.getFileName();
        return (fileName == null) ? "" : fileName.toString();
    }

    @Override
    public FileReference getThemeConfig() {
        Path themeConfig = path.resolve(FILE_NAME_THEME);
        if (Files.exists(themeConfig)) {
            return new ArtifactFileReference(themeConfig, appReference);
        } else {
            throw new UUFException("Theme '" + getName() + "' of app '" + appReference.getPath() + "' is empty.");
        }
    }
}
