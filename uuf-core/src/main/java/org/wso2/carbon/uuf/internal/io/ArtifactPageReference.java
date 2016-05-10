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

package org.wso2.carbon.uuf.internal.io;

import org.apache.commons.io.FilenameUtils;
import org.wso2.carbon.uuf.reference.ComponentReference;
import org.wso2.carbon.uuf.reference.FileReference;
import org.wso2.carbon.uuf.reference.PageReference;

import java.nio.file.Path;

public class ArtifactPageReference implements PageReference {

    private final Path path;
    private final ArtifactComponentReference componentReference;

    public ArtifactPageReference(Path path, ArtifactComponentReference componentReference) {
        this.path = path;
        this.componentReference = componentReference;
    }

    @Override
    public String getPathPattern() {
        StringBuilder sb = new StringBuilder();
        Path pagesDirectory = componentReference.getPath().resolve(ComponentReference.DIR_NAME_PAGES).relativize(path);
        for (Path path : pagesDirectory) {
            sb.append('/').append(FilenameUtils.removeExtension(path.toString()));
        }
        return sb.toString();
    }

    @Override
    public FileReference getRenderingFile() {
        return new ArtifactFileReference(path, componentReference.getAppReference());
    }
}
