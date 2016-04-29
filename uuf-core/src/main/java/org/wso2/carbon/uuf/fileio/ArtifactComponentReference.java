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

import org.wso2.carbon.uuf.core.create.ComponentReference;
import org.wso2.carbon.uuf.core.create.FileReference;
import org.wso2.carbon.uuf.core.create.FragmentReference;
import org.wso2.carbon.uuf.core.create.LayoutReference;
import org.wso2.carbon.uuf.core.create.PageReference;
import org.wso2.carbon.uuf.core.exception.UUFException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ArtifactComponentReference implements ComponentReference {

    private final Path path;

    public ArtifactComponentReference(Path path) {
        this.path = path;
    }

    @Override
    public Stream<PageReference> getPages(Set<String> supportedExtensions) {
        Path pages = path.resolve(DIR_NAME_PAGES);
        if (!Files.exists(pages)) {
            return Stream.<PageReference>empty();
        }
        try {
            return Files
                    .walk(pages)
                    .filter(path -> Files.isRegularFile(path) &&
                            supportedExtensions.contains(getExtension(path.getFileName().toString())))
                    .map(path -> new ArtifactPageReference(path, this));
        } catch (IOException e) {
            throw new UUFException("An error occurred while listing pages in '" + path + "'.", e);
        }
    }

    @Override
    public Stream<LayoutReference> getLayouts(Set<String> supportedExtensions) {
        Path layouts = path.resolve(DIR_NAME_LAYOUTS);
        if (!Files.exists(layouts)) {
            return Stream.<LayoutReference>empty();
        }
        try {
            return Files
                    .list(layouts)
                    .filter(path -> Files.isRegularFile(path) &&
                            supportedExtensions.contains(getExtension(path.getFileName().toString())))
                    .map(path -> new ArtifactLayoutReference(path, this));
        } catch (IOException e) {
            throw new UUFException("An error occurred while listing layouts in '" + path + "'.", e);
        }
    }

    private String getExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
    }

    @Override
    public Stream<FragmentReference> getFragments(Set<String> supportedExtensions) {
        Path fragments = path.resolve(DIR_NAME_FRAGMENTS);
        if (!Files.exists(fragments)) {
            return Stream.<FragmentReference>empty();
        }
        try {
            return Files
                    .list(fragments)
                    .filter(Files::isDirectory)
                    .map(path -> new ArtifactFragmentReference(path, this, supportedExtensions));
        } catch (IOException e) {
            throw new UUFException("An error occurred while listing fragments in '" + path + "'.", e);
        }
    }

    @Override
    public Optional<FileReference> getBindingsConfig() {
        Path bindingsConfiguration = path.resolve(FILE_NAME_BINDINGS);
        if (Files.exists(bindingsConfiguration)) {
            return Optional.of(new ArtifactFileReference(bindingsConfiguration, this));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileReference> getConfigurations() {
        Path configuration = path.resolve(FILE_NAME_CONFIGURATIONS);
        if (Files.exists(configuration)) {
            return Optional.of(new ArtifactFileReference(configuration, this));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileReference> getOsgiImportsConfig() {
        Path binding = path.resolve(FILE_NAME_OSGI_IMPORTS);
        if (Files.exists(binding)) {
            return Optional.of(new ArtifactFileReference(binding, this));
        } else {
            return Optional.empty();
        }
    }

    Path getPath() {
        return path;
    }
}
