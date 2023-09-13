package com.nil;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.util.ArrayList;
import java.util.List;

public class NileshDPExtension {
    Property<Boolean> ignoreVersions;
    Property<Boolean> beautify;
    Property<String> path;
    ListProperty<String> exclude;

    public void setIgnoreVersions(boolean ignoreVersions) {
        this.ignoreVersions.set(ignoreVersions);
    }

    public void setBeautify(boolean beautify) {
        this.beautify.set(beautify);
    }

    public void setPath(String path) {
        this.path.set(path);
    }

    public void setExclude(List<String> exclude) {
        this.exclude.set(exclude);
    }
    public void addExclude(String exclude) {
        this.exclude.add(exclude);
    }

    @javax.inject.Inject
    public NileshDPExtension(ObjectFactory objects) {
        ignoreVersions = objects.property(boolean.class);
        beautify = objects.property(boolean.class);
        path = objects.property(String.class);
        exclude = objects.listProperty(String.class);
    }

    //all the getters with default values in case it's run without any configuration
    public List<String> getExclude(){
        return exclude.getOrElse(new ArrayList<>());
    }
    public boolean isIgnoreVersions(){
        return ignoreVersions.getOrElse(true);
    }
    public boolean isBeautify(){
        return beautify.getOrElse(true);
    }
    public String getPath(){
        return path.getOrElse("NilDP.json");
    }
}
