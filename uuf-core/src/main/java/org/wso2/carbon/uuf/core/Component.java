package org.wso2.carbon.uuf.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Component {
    private static final Logger log = LoggerFactory.getLogger(Component.class);

    private final String name;
    private final String context;
    private final List<Page> pages;
    private final Map<String, Fragment> fragments;
    private final Map<String, String> configuration;
    private final Map<String, Renderable> bindings;

    public Component(String name, Set<Page> pages, Set<Fragment> fragments, Map<String, String> config,
                     Map<String, String> bindingsConfig) {
        this(name, getContextFormName(name), pages, fragments, config, bindingsConfig);
    }

    public Component(String name, String context, Set<Page> pages, Set<Fragment> fragments, Map<String, String> config,
                     Map<String, String> bindingsConfig) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Component name cannot be empty.");
        }
        this.name = name;
        this.context = context;
        this.pages = pages.stream().collect(Collectors.toList());
        //TODO: use a sorted Set and avoid sorting
        // We sort pages based on their URIs so that more wildcard-ed ones go to the bottom.
        Collections.sort(this.pages, (p1, p2) -> p1.getUriPatten().compareTo(p2.getUriPatten()));
        this.fragments = fragments.stream().collect(Collectors.toMap(Fragment::getName, fragment -> fragment));
        this.configuration = config;
        this.bindings = new HashMap<>(bindingsConfig.size());
        for (Map.Entry<String, String> entry : bindingsConfig.entrySet()) {
            Fragment fragment = this.fragments.get(entry.getValue());
            if (fragment == null) {
                throw new UUFException("Fragment '" + entry.getValue() + "' does not exists in Component '" + name +
                        "'. Hence cannot bind it to zone '" + entry.getKey() + "'.");
            }
            bindings.put(entry.getKey(), fragment.getRenderer());
        }
    }

    private static String getContextFormName(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            return name.substring(lastDot);
        } else {
            return name;
        }
    }

    public Map<String, Renderable> getBindings() {
        return bindings;
    }

    public String getContext() {
        return context;
    }

    public Optional<String> renderPage(String pageUri) {
        Optional<Page> servingPage = getPage(pageUri);
        if (log.isDebugEnabled() && servingPage.isPresent()) {
            log.debug("Component '" + name + "' is serving Page '" +
                    servingPage.get().toString() + "' for URI '" + pageUri + "'.");
        }
        return servingPage.map(page -> page.serve(createModel(pageUri), bindings, fragments));
    }

    private Map<String, Object> createModel(String pageUri) {
        Map<String, Object> model = new HashMap<>();
        model.put("pageUri", pageUri);
        model.put("config", configuration);
        return model;
    }

    private Optional<Page> getPage(String pageUri) {
        return pages.stream().filter(page -> page.getUriPatten().match(pageUri)).findFirst();
    }

    public boolean hasPage(String uri) {
        return getPage(uri).isPresent();
    }

    public String getName() {
        return name;
    }

    public Set<Page> getPages() {
        //TODO: convert pages to a set
        return new HashSet<>(pages);
    }

    public Map<String, Fragment> getFragments() {
        return fragments;
    }
}
