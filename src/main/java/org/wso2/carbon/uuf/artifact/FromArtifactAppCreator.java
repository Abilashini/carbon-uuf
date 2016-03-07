package org.wso2.carbon.uuf.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.uuf.core.App;
import org.wso2.carbon.uuf.core.AppCreator;
import org.wso2.carbon.uuf.core.Page;
import org.wso2.carbon.uuf.core.UUFException;

import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FromArtifactAppCreator implements AppCreator {

    private static final Logger log = LoggerFactory.getLogger(FromArtifactAppCreator.class);
    private final String[] paths;
    private final PageCreator pageCreator = new PageCreator();

    public FromArtifactAppCreator(String[] paths) {
        this.paths = paths;
    }

    private static Stream<Path> pagesOfAComponent(Path componentDir) {
        try {
            Path pagesDir = componentDir.resolve("pages");
            if (Files.isDirectory(pagesDir)) {
                return Files.list(pagesDir);
            } else {
                return Stream.empty();
            }
        } catch (IOException e) {
            throw new UUFException("error while finding the pages", e);
        }
    }

    private App createFromComponents(Path components, String context) throws IOException {
        if (!Files.exists(components)) {
            throw new FileNotFoundException("components dir must exist in a build artifact");
        }

        LayoutCreator layoutCreator = new LayoutCreator(components);
        List<Page> pages = Files
                .list(components)
                .flatMap(FromArtifactAppCreator::pagesOfAComponent)
                .map(pageDir -> pageCreator.createPage(pageDir, layoutCreator))
                .collect(Collectors.toList());

        return new App(context, pages, Collections.emptyList());
    }


    @Override
    public App createApp(String name, String context) {
        // app list mush be <white-space> and comma separated. <white-space> in app names not allowed
        for (String pathString : paths) {
            Path path = FileSystems.getDefault().getPath(pathString);
            if (name.equals(path.toAbsolutePath().normalize().getFileName().toString())) {
                try {
                    return createFromComponents(path.resolve("components"), context);
                } catch (IOException e) {
                    throw new UUFException("error while creating app for '" + name + "'", e);
                }
            }
        }
        throw new UUFException(
                "app by the name '" + name + "' is not found in " + Arrays.toString(paths),
                Response.Status.NOT_FOUND);
    }
}
