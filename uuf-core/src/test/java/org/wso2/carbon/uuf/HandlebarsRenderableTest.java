package org.wso2.carbon.uuf;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.uuf.core.Fragment;
import org.wso2.carbon.uuf.core.Renderable;
import org.wso2.carbon.uuf.handlebars.Executable;
import org.wso2.carbon.uuf.handlebars.HbsRenderable;
import org.wso2.carbon.uuf.handlebars.JSExecutable;

import java.util.Collections;
import java.util.Map;

public class HandlebarsRenderableTest {

    @Test
    public void testTemplate() {
        final String templateContent = "A Plain Handlebars template.";
        HbsRenderable hbsRenderable = new HbsRenderable(templateContent);
        String output = hbsRenderable.render(new Object(), ImmutableListMultimap.of(), Collections.emptyMap());
        Assert.assertEquals(output, templateContent);
    }

    @Test
    public void testTemplateWithModel() {
        HbsRenderable hbsRenderable = new HbsRenderable("Hello {{name}}! Have a good day.");
        Map model = ImmutableMap.of("name", "Alice");
        String output = hbsRenderable.render(model, ImmutableListMultimap.of(), Collections.emptyMap());
        Assert.assertEquals(output, "Hello Alice! Have a good day.");
    }

    @Test
    public void testTemplateWithExecutable() {
        Executable executable = context -> ImmutableMap.of("name", "Alice");
        HbsRenderable hbsRenderable = new HbsRenderable("Hello {{name}}! Have a good day.", executable);
        String output = hbsRenderable.render(new Object(), ImmutableListMultimap.of(), Collections.emptyMap());
        Assert.assertEquals(output, "Hello Alice! Have a good day.");
    }

    @Test
    public void testTemplateWithJsExecutable() {
        JSExecutable script = new JSExecutable("function onRequest(){ return {name: \"Alice\"}; }");
        HbsRenderable hbsRenderable = new HbsRenderable("Hello {{name}}! Have a good day.", script);
        String output = hbsRenderable.render(new Object(), ImmutableListMultimap.of(), Collections.emptyMap());
        Assert.assertEquals(output, "Hello Alice! Have a good day.");
    }

    @Test
    public void testFragment() {
        HbsRenderable hbsRenderable = new HbsRenderable("{{includeFragment \"test-fragment\"}}");
        final String fragmentContent = "This is the content of the test-fragment.";
        HbsRenderable fragmentRenderable = new HbsRenderable(fragmentContent);
        Fragment fragment = new Fragment("test-fragment", "/mock/path", fragmentRenderable);
        String output = hbsRenderable.render(new Object(), ImmutableListMultimap.of(), ImmutableMap.of("test-fragment",
                                                                                                       fragment));
        Assert.assertEquals(output, fragmentContent);
    }

    @Test
    public void testZones() {
        HbsRenderable defineZoneRenderable = new HbsRenderable("{{defineZone \"test-zone\"}}");
        final String zoneContent = "This is the content of the test-zone.";
        HbsRenderable fillZoneRenderable = new HbsRenderable(zoneContent);
        Multimap<String, Renderable> bindings = ImmutableListMultimap.of("test-zone", fillZoneRenderable);
        String output = defineZoneRenderable.render(new Object(), bindings, Collections.emptyMap());
        Assert.assertEquals(output, zoneContent);
    }
}
