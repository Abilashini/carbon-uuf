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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.wso2.carbon.uuf.exception.UUFException;
import org.wso2.carbon.uuf.reference.ComponentReference;
import org.wso2.carbon.uuf.reference.FileReference;
import org.wso2.carbon.uuf.reference.FragmentReference;
import org.wso2.carbon.uuf.reference.LayoutReference;
import org.wso2.carbon.uuf.reference.PageReference;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.FileReader;

public class ArtifactComponentReference implements ComponentReference {

    private final Path path;
    private final ArtifactAppReference appReference;

    public ArtifactComponentReference(Path path, ArtifactAppReference appReference) {
        this.path = path;
        this.appReference = appReference;
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
            return Optional.of(new ArtifactFileReference(bindingsConfiguration, appReference));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileReference> getConfigurations() {
        Path configuration = path.resolve(FILE_NAME_CONFIGURATIONS);
        if (Files.exists(configuration)) {
            return Optional.of(new ArtifactFileReference(configuration, appReference));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileReference> getOsgiImportsConfig() {
        Path binding = path.resolve(FILE_NAME_OSGI_IMPORTS);
        if (Files.exists(binding)) {
            return Optional.of(new ArtifactFileReference(binding, appReference));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String getPath() {
        return path.toString();
    }

    Path getFilePath() {
        return path;
    }

    ArtifactAppReference getAppReference() {
        return appReference;
    }

    @Override
    public Map<String, Properties> getI18nFiles(){
        Path lang = path.resolve(DIR_NAME_LANGUAGE);
        Map<String, Properties> i18n = new HashMap<>();

        if (!Files.exists(lang)) {
            return i18n;
        }

       //if (Files.exists(lang)) {
            //list all lang files within dir
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(lang, "*.{properties}")) {
                for (Path entry: stream) {
                    if(Files.isRegularFile(entry)){
                        Properties props = new Properties();
                        props.load(new FileReader(entry.toString()));

                        i18n.put(entry.getFileName().toString().substring(0,entry.getFileName().toString().indexOf('.')), props);
                    }
                }
            } catch (DirectoryIteratorException ex) {
                // I/O error encounted during the iteration, the cause is an IOException
                //throw ex.getCause();
            } catch (IOException ex) {
            // I/O error encounted during the iteration, the cause is an IOException
            //throw ex.getCause();
            }
        //}


       return i18n;
    }
}
