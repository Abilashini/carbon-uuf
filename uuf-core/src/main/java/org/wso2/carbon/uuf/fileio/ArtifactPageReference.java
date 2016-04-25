package org.wso2.carbon.uuf.fileio;

import org.apache.commons.io.FilenameUtils;
import org.wso2.carbon.uuf.core.create.AppReference;
import org.wso2.carbon.uuf.core.create.ComponentReference;
import org.wso2.carbon.uuf.core.create.FileReference;
import org.wso2.carbon.uuf.core.create.PageReference;

import java.nio.file.Path;

public class ArtifactPageReference implements PageReference {

    private final Path path;
    private final ArtifactAppReference appReference;
    private final ArtifactComponentReference componentReference;

    public ArtifactPageReference(Path path, ArtifactComponentReference componentReference,
                                 ArtifactAppReference appReference) {
        this.path = path;
        this.componentReference = componentReference;
        this.appReference = appReference;
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
        return new ArtifactFileReference(path, componentReference);
    }

    @Deprecated
    @Override
    public AppReference getAppReference() {
        return appReference;
    }

    @Deprecated
    @Override
    public ComponentReference getComponentReference() {
        return componentReference;
    }
}
