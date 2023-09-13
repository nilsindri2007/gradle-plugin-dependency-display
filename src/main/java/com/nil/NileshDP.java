package com.nil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.file.CopySpec;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Copy;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class NileshDP implements Plugin <Project>{
    @Override
    public void apply(Project project) {
        //get plugin settings
        NileshDPExtension settings = project.getExtensions().create("nildp", NileshDPExtension.class);
        //register task
        project.task("generate-dependency-list").doLast(task->{
            //populate the list of dependencies we don't want to have
            List<ResolvedDependency> excluded = new ArrayList<>();
            Logger logger = project.getLogger();
            logger.info("Top-level dependencies: ");
            task.getProject().getConfigurations().getByName("runtimeClasspath").getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(resolvedDependency -> {
                if(shouldExclude(settings.getExclude(), settings.isIgnoreVersions(), resolvedDependency)){
                    logger.info("Excluding "+resolvedToString(resolvedDependency)+" and it's dependencies");
                    excluded.addAll(flatten(resolvedDependency));
                }else{
                    logger.info("Including "+resolvedToString(resolvedDependency)+" and it's dependencies");
                }
            });

            logger.info("All resolved dependencies: ");
            List<String> resolved = new ArrayList<>();
            task.getProject()
                    .getConfigurations().getByName("runtimeClasspath")
                    .getIncoming().getArtifacts().getArtifacts()
                    .forEach(artifact->{
                        if(excluded.stream().anyMatch(ex->areSame(settings.isIgnoreVersions(), resolvedToString(ex), artifact.getId().getComponentIdentifier().toString()))){
                            logger.info("Excluding "+artifact.getId().getComponentIdentifier().toString());
                        }else{
                            logger.info("Including "+artifact.getId().getComponentIdentifier().toString());
                            resolved.add(artifact.getId().getComponentIdentifier().toString());
                        }
            });

            JsonArray resultingArray = new JsonArray();
            for(String selector:resolved){
                JsonObject element = new JsonObject();
                String[] dependency = selector.split(":");
                element.addProperty("groupId",dependency[0]);
                element.addProperty("artifactId",dependency[1]);
                element.addProperty("version",dependency[2]);
                resultingArray.add(element);
            }

            Path file = Paths.get(settings.getPath()); //relative file in target jar
            Path parent = file.getParent()!=null?file.getParent():Paths.get(""); //if it has parent directory we want to know that
            File result = new File(new File(project.getBuildDir(),"NilDP"), settings.getPath()); //temporary folder for the json file
            if(result.getParentFile()!=null)
                result.getParentFile().mkdirs();

            try {
                FileWriter writer = new FileWriter(result);
                Gson gson = settings.isBeautify()?new GsonBuilder().setPrettyPrinting().create():new Gson(); //beautify if needed
                gson.toJson(resultingArray, writer);
                writer.flush(); //save the file
                writer.close();
                logger.info("Total number of dependencies written to file: "+resultingArray.size());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //copy freshly generated file into target jar
            project.getTasks().named("processResources", Copy.class, t -> {
                CopySpec copyPluginDescriptors = t.getRootSpec().addChild();
                copyPluginDescriptors.into(parent.toString());
                copyPluginDescriptors.from(result.toString());
            });

        });
    }

    private boolean shouldExclude(List<String> toExclude, boolean ignoreVersion, ResolvedDependency dependency){
        for(String e:toExclude){
            String[] exclude = e.split(":");
            if(exclude[0].equals(dependency.getModuleGroup())&&exclude[1].equals(dependency.getModuleName())&&(ignoreVersion||exclude[2].equals(dependency.getModuleVersion()))){
                return true;
            }
        }
        return false;
    }

    private  boolean areSame(boolean ignoreVersion, String one, String other){
        String[] a = one.split(":");
        String[] b = other.split(":");
        return  a[0].equals(b[0])&&a[1].equals(b[1])&&(ignoreVersion||a[2].equals(b[2]));
    }

    @SuppressWarnings("unused")
    private boolean areSame(boolean ignoreVersion, ResolvedDependency resolved, DependencyResult res){
        return areSame(ignoreVersion, resolved.getModuleGroup()+":"+resolved.getModuleName()+":"+resolved.getModuleVersion(), res.getRequested().toString());
    }

    private String resolvedToString(ResolvedDependency d){
        return d.getModuleGroup()+":"+d.getModuleName()+":"+d.getModuleVersion();
    }

    @SuppressWarnings("unused")
    private void printChildren(ResolvedDependency resolvedDependency, String offset){
        resolvedDependency.getChildren().forEach(child->{
            System.out.println(offset+resolvedToString(child)+" "+child.getConfiguration());
            printChildren(child, offset+"-");
        });
    }

    private List<ResolvedDependency> flatten(ResolvedDependency resolvedDependency){
        List<ResolvedDependency> result = new ArrayList<>();
        result.add(resolvedDependency);
        resolvedDependency.getChildren().forEach(d->result.addAll(flatten(d)));
        return result;
    }

}
