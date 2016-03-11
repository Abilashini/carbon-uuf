package org.wso2.carbon.uuf.fileio;

import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateSource;
import org.wso2.carbon.uuf.core.Executable;
import org.wso2.carbon.uuf.core.HandlebarsRenderble;
import org.wso2.carbon.uuf.core.JSExecutable;
import org.wso2.carbon.uuf.core.Renderble;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class FileUtil {

    static Path relativePath(Path path) {
        path = path.normalize();
        int pageIndex = -1;
        int count = path.getNameCount();
        for (int i = count; i >= 1; i--) {
            Path subPath = path.subpath(i - 1, i);
            if (subPath.toString().equals("pages") && i > 1 && pageIndex == -1) {
                pageIndex = i;
            }
            if (subPath.toString().equals("components") && i > 1) {
                return path.subpath(i - 2, count);
            }
        }
        if (pageIndex > -1) {
            return path.subpath(pageIndex - 2, count);
        }
        return path;
    }

    static Renderble createRenderble(Path hbsFile) throws IOException {
        Renderble template;
        String content = new String(Files.readAllBytes(hbsFile), StandardCharsets.UTF_8);
        TemplateSource source = new StringTemplateSource(
                relativePath(hbsFile).toString(),
                content);
        template = new HandlebarsRenderble(source);
        return template;
    }

    @Nullable
    static Executable createExecutable(Path jsFile) throws IOException {
        if (Files.isRegularFile(jsFile)) {

            Executable executable;
            executable = new JSExecutable(
                    new String(Files.readAllBytes(jsFile)),
                    FileUtil.relativePath(jsFile).toString());
            return executable;
        } else {
            return null;
        }
    }
}
