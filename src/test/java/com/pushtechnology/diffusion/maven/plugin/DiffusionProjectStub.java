package com.pushtechnology.diffusion.maven.plugin;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * @author Phil Aston
 * @author Andy Piper
 */
public class DiffusionProjectStub extends MavenProject {

    public DiffusionProjectStub(final File buildDirectory, final File pom)
            throws Exception {

        super(new DiffusionModelStub());

        setExecutionProject(this);

        setFile(pom);

        final Build build = new Build();
        build.setDirectory(buildDirectory.getAbsolutePath());
        setBuild(build);
        setDependencyArtifacts((Set)emptySet());
        setArtifacts((Set)emptySet());
        setPluginArtifacts((Set)emptySet());
        setReportArtifacts((Set)emptySet());
        setExtensionArtifacts((Set)emptySet());
        setRemoteArtifactRepositories((List)emptyList());
        setPluginArtifactRepositories((List)emptyList());
        setCollectedProjects((List)emptyList());
        setActiveProfiles((List)emptyList());
    }
}
