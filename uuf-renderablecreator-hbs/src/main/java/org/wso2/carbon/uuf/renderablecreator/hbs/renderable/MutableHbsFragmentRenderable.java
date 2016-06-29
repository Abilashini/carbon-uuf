package org.wso2.carbon.uuf.renderablecreator.hbs.renderable;

import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.TemplateSource;
import org.wso2.carbon.uuf.renderablecreator.hbs.core.Executable;

import java.util.Optional;

public class MutableHbsFragmentRenderable extends HbsFragmentRenderable {

    private final String path;
    private volatile Template template;
    private volatile Executable executable;

    public MutableHbsFragmentRenderable(TemplateSource templateSource) {
        this(templateSource, null);
    }

    public MutableHbsFragmentRenderable(TemplateSource templateSource, Executable executable) {
        this.template = compile(templateSource);
        this.path = templateSource.filename();
        this.executable = executable;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @Override
    public Optional<Executable> getExecutable() {
        return Optional.ofNullable(executable);
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }
}
