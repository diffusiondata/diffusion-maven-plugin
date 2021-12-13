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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.pushtechnology.diffusion.api.config.ConnectorConfig;
import com.pushtechnology.diffusion.api.config.ServerConfig;
import com.pushtechnology.diffusion.api.server.EmbeddedDiffusion;

/**
 * Runs Diffusion directly from a Maven project from a binding to an execution
 * in your POM.
 */
@Execute(phase = LifecyclePhase.PRE_INTEGRATION_TEST)
@Mojo(
    name = "start",
    defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
    requiresDependencyResolution=ResolutionScope.TEST)
public class DiffusionStartMojo extends AbstractDiffusionMojo {

    /**
     * Number of milliseconds to wait for the Diffusion server to start.
     */
    @Parameter(defaultValue = "60000")
    protected long serverStartTimeout = 60000;

    /**
     * Whether to wait for Diffusion server deployments to finish before declaring success
     */
    @Parameter(defaultValue = "true")
    protected boolean waitForDeployments = true;

    @Parameter(property = "diffuson.home")
    protected String diffusionHome;

    /**
     * Comma-separated list of Diffusion XML configuration files whose contents
     * will be applied before any plugin configuration. Optional.
     */
    @Parameter(alias = "diffusionConfig")
    protected String diffusionConfigDir;

    /**
     * Diffusion server port
     */
    @Parameter(defaultValue = "8080")
    protected int port;

    /**
     * Diffusion server SSL port
     */
    @Parameter(defaultValue = "8443")
    protected int sslPort;

    /**
     * Diffusion maxMessageSize
     */
    @Parameter(defaultValue = "32768")
    protected int maxMessageSize;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping Diffusion start: diffusion.skip==true");
            return;
        }
        if (this.project == null) {
            throw new MojoExecutionException("No project could be found");
        }
        getLog().info("Configuring Diffusion for project: " + this.project.getName());

        configurePluginClasspath();

        PluginLog.setLog(getLog());

        if (diffusionConfigDir != null) {
            File configDir = new File(diffusionConfigDir);
            if (!configDir.exists() || !configDir.isDirectory()) {
                throw new MojoExecutionException("Invalid diffusionConfigDir");
            }
        }

        final EmbeddedDiffusion server = startDiffusion(configureServerClasspath());

        if (waitForDeployments) {
            final CountDownLatch startLock = new CountDownLatch(1);
            server.addLifecycleListener(new EmbeddedDiffusion.LifecycleListener() {
                @Override
                public void onStateChanged(EmbeddedDiffusion.State state) {
                    if (state == EmbeddedDiffusion.State.STARTED) {
                        startLock.countDown();
                    }
                }
            });

            try {
                if (!startLock.await(serverStartTimeout, TimeUnit.MILLISECONDS)) {
                    throw new MojoExecutionException("Server failed to start after " + serverStartTimeout / 1000 + "s");
                }
            }
            catch (InterruptedException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        getPluginContext().put("startedServerInstance", server);
    }

    /**
     * Check if a port is available. From SO.
     */
    private static boolean portAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }

        return false;
    }

    private void finishConfigurationBeforeStart(ServerConfig config) throws Exception {
        ConnectorConfig connector = config.getConnector("Client Connector");
        config.getManagement().setEnabled(false);
        if (connector != null) {
            if (!portAvailable(port)) {
                throw new MojoExecutionException("Port " + port + " is not available and thus the server will not be able to start");
            }
            connector.setPort(port);
        }
        connector = config.getConnector("HTTP Connector");
        if (connector != null) {
            if (!portAvailable(port)) {
                throw new MojoExecutionException("Port " + port + " is not available and thus the server will not be able to start");
            }
            connector.setPort(port);
        }
        connector = config.getConnector("SSL Connector");
        if (connector != null) {
            if (!portAvailable(sslPort)) {
                throw new MojoExecutionException("Port " + sslPort + " is not available and thus the server will not be able to start");
            }
            connector.setPort(sslPort);
        }
        config.setMaximumMessageSize(maxMessageSize);
    }

    private void printSystemProperties() {
        // print out which system properties were set up
        if (getLog().isDebugEnabled()) {
            if (systemProperties != null) {
                final Iterator<SystemProperty> itor = systemProperties.getSystemProperties().iterator();
                while (itor.hasNext()) {
                    SystemProperty prop = itor.next();
                    getLog().debug("Property " + prop.getName() + "=" + prop.getValue() + " was " + (prop.isSet() ? "set" : "skipped"));
                }
            }
        }
    }

    private Properties configureSystemProperties() throws MojoExecutionException {
        Properties props = new Properties();
        if (systemProperties != null) {
            final Iterator<SystemProperty> itor = systemProperties.getSystemProperties().iterator();
            while (itor.hasNext()) {
                SystemProperty prop = itor.next();
                props.put(prop.getName(), prop.getValue());
            }
        }

        if (diffusionHome != null && diffusionHome.length() > 0) {
            props.setProperty("diffusion.home", diffusionHome);
        }
        // Fix up diffusion.home from environment if set.
        else if (props.getProperty("diffusion.home") == null && System.getenv("DIFFUSION_HOME") != null) {
            diffusionHome = System.getenv("DIFFUSION_HOME");
            props.setProperty("diffusion.home", diffusionHome);
        }
        else {
            diffusionHome = props.getProperty("diffusion.home");
        }

        return props;
    }

    private EmbeddedDiffusion startDiffusion(ClassLoader serverClassLoader) throws MojoExecutionException {
        try {
            getLog().debug("Starting Diffusion Server ...");

            printSystemProperties();

            final EmbeddedDiffusion
                server = DiffusionServerWrapper.createServer(configureSystemProperties(), serverClassLoader);

            finishConfigurationBeforeStart(server.getConfig());

            server.start();
            getLog().info("Started Diffusion Server");
            return server;
        }
        catch (MojoExecutionException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MojoExecutionException("Failed to start Diffusion", e);
        }
    }

    private void configurePluginClasspath() throws MojoExecutionException {
        // If we are configured to include the provided dependencies on the plugin's classpath,
        // we first try and filter out ones that will clash with jars that are plugin dependencies,
        // then create a new classloader that we setup in the parent chain.
        if (useProvidedScope) {
            try {
                List<URL> provided = new ArrayList<>();
                URL[] urls = null;

                for (Iterator<Artifact> iter = projectArtifacts.iterator(); iter.hasNext(); ) {
                    Artifact artifact = iter.next();
                    if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) && !isPluginArtifact(artifact)) {
                        provided.add(artifact.getFile().toURI().toURL());
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Adding provided artifact: " + artifact);
                        }
                    }
                }

                if (!provided.isEmpty()) {
                    urls = new URL[provided.size()];
                    provided.toArray(urls);
                    URLClassLoader loader = new URLClassLoader(urls, null);
                    Thread.currentThread().setContextClassLoader(loader);
                    getLog().info("Plugin classpath augmented with <scope>provided</scope> dependencies: " + Arrays.toString(urls));
                }
            }
            catch (MalformedURLException e) {
                throw new MojoExecutionException("Invalid url", e);
            }
        }
    }

    private ClassLoader configureServerClasspath() throws MojoExecutionException {

        try {
            List<URL> provided = new ArrayList<>();
            URL[] urls = null;

            for (Iterator<Artifact> iter = pluginArtifacts.iterator(); iter.hasNext(); ) {
                Artifact artifact = iter.next();
                provided.add(artifact.getFile().toURI().toURL());
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Adding provided artifact: " + artifact);
                }
            }

            urls = new URL[provided.size()];
            provided.toArray(urls);

            // A small subset of classes necessary for communicating with
            // the Diffusion interface.
            final Set<String> sharedClasses = new HashSet<>(asList(
                    EmbeddedDiffusion.class.getName(),
                    EmbeddedDiffusion.LifecycleListener.class.getName(),
                    EmbeddedDiffusion.State.class.getName(),
                    "com.pushtechnology.diffusion.api.conflation.*",
                    // Needed by Diffusion 6.7 and earlier:
                    "com.pushtechnology.diffusion.api.LogDescription",
                    "com.pushtechnology.diffusion.api.LogDescription$LogLevel",
                    "com.pushtechnology.diffusion.api.config.*",
                    // Allow JAXB to be packaged as a library.
                    "javax.xml.*",
                    "com.sun,*"
            ));

            getLog().info("Plugin classpath set to : " + Arrays.toString(urls));

            return new BlockingClassLoader(provided,
                    Collections.<String>emptySet(),
                    singleton("*"),
                    sharedClasses,
                    true);
        }
        catch (MalformedURLException e) {
            throw new MojoExecutionException("Invalid url", e);
        }
    }

    private boolean isPluginArtifact(Artifact artifact) {
        if (pluginArtifacts == null || pluginArtifacts.isEmpty()) {
            return false;
        }

        boolean isPluginArtifact = false;
        for (Iterator<Artifact> iter = pluginArtifacts.iterator(); iter.hasNext() && !isPluginArtifact; ) {
            Artifact pluginArtifact = iter.next();
            if (getLog().isDebugEnabled()) {
                getLog().debug("Checking " + pluginArtifact);
            }
            if (pluginArtifact.getGroupId().equals(artifact.getGroupId()) && pluginArtifact.getArtifactId().equals(artifact.getArtifactId())) {
                isPluginArtifact = true;
            }
        }

        return isPluginArtifact;
    }
}
