/*
 * Copyright (C) 2015, 2021 Push Technology Ltd.
 * Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Derived in part from the jetty-maven-plugin and redistributed under ASL 2.0.
 */
package com.pushtechnology.diffusion.maven.plugin;

import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.pushtechnology.diffusion.api.server.EmbeddedDiffusion;

/**
 * Common base class for most Diffusion mojos.
 */
public abstract class AbstractDiffusionMojo extends AbstractMojo {
    /**
     * Whether or not to include dependencies on the plugin's classpath with &lt;scope&gt;provided&lt;/scope&gt;
     * Use with caution. This can cause duplicate jars/classes.
     */
    @Parameter(defaultValue = "false")
    protected boolean useProvidedScope;

    /**
     * List of goals that are not to be used
     */
    @Parameter
    protected String[] excludedGoals;

    /**
     * System properties to set before execution.
     * Note that these properties will not override system properties
     * that have been set on the command line or by the JVM.
     * They will override system properties that have been set via systemPropertiesFile.
     * Optional.
     */
    @Parameter
    protected SystemProperties systemProperties;

    /**
     * Skip this mojo execution.
     */
    @Parameter(property = "diffusion.skip", defaultValue = "false")
    protected boolean skip;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.artifacts}", readonly = true, required = true)
    protected Set<Artifact> projectArtifacts;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    protected org.apache.maven.plugin.MojoExecution execution;

    @Parameter(defaultValue = "${plugin.artifacts}", readonly = true, required = true)
    protected List<Artifact> pluginArtifacts;

    protected final EmbeddedDiffusion getServer() {
        return (EmbeddedDiffusion) getPluginContext().get("startedServerInstance");
    }

    protected final void stopDiffusion() throws MojoExecutionException {
        final EmbeddedDiffusion server = getServer();

        if (server != null) {
            try {
                server.stop();
                getLog().info("Stopped Diffusion Server");
            }
            catch (Exception e) {
                throw new MojoExecutionException("Failure", e);
            }
        }
        else {
            getLog().warn("Diffusion Server never started");
        }
    }
}
