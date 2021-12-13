/*
 * Copyright (C) 2015 Push Technology Ltd.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.pushtechnology.diffusion.api.config.ConnectorConfig;
import com.pushtechnology.diffusion.api.config.ServerConfig;
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
     * File containing system properties to be set before execution
     * <p/>
     * Note that these properties will not override system properties
     * that have been set on the command line, by the JVM, or directly
     * in the POM via systemProperties. Optional.
     */
    @Parameter(property = "diffusion.systemPropertiesFile")
    protected File systemPropertiesFile;

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
     * Directory to store Diffusion log files.
     * This relies on features only available in Diffusion 5.6 or later.
     */
    @Parameter(defaultValue = "${project.build.directory}/diffusion-server")
    protected File logDirectory;

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

    /**
     * Use the dump() facility of Diffusion to print out the server configuration to logging
     */
    @Parameter(property = "dumponStart", defaultValue = "false")
    protected boolean dumpOnStart;

    /**
     * Skip this mojo execution.
     */
    @Parameter(property = "diffusion.skip", defaultValue = "false")
    protected boolean skip;

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

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.artifacts}", readonly = true, required = true)
    protected Set projectArtifacts;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    protected org.apache.maven.plugin.MojoExecution execution;

    @Parameter(defaultValue = "${plugin.artifacts}", readonly = true, required = true)
    protected List pluginArtifacts;

    /**
     * A wrapper for the Server object
     */
    protected EmbeddedDiffusion server;

    @Parameter(property = "diffuson.home")
    protected String diffusionHome;

    protected ClassLoader serverClassLoader;

    public abstract void checkPomConfiguration() throws MojoExecutionException;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.project == null) {
            throw new MojoExecutionException("No project could be found");
        }
        getLog().info("Configuring Diffusion for project: " + this.project.getName());
        if (skip) {
            getLog().info("Skipping Diffusion start: diffusion.skip==true");
            return;
        }

        if (isExcluded(execution.getMojoDescriptor().getGoal())) {
            getLog().info("The goal \"" + execution.getMojoDescriptor().getFullGoalName() +
                    "\" has been made unavailable for this web application by an <excludedGoal> configuration.");
            return;
        }

        configurePluginClasspath();
        PluginLog.setLog(getLog());
        checkPomConfiguration();
        startDiffusion();
    }

    public Properties configureSystemProperties() throws MojoExecutionException {
        Properties props = new Properties();
        if (systemProperties != null) {
            final Iterator itor = systemProperties.getSystemProperties().iterator();
            while (itor.hasNext()) {
                SystemProperty prop = (SystemProperty) itor.next();
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

        if (logDirectory != null) {
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }
            else if (!logDirectory.isDirectory()) {
                throw new MojoExecutionException("Invalid log directory: " + logDirectory);
            }
            if (props.getProperty("diffusion.log.dir") == null) {
                props.setProperty("diffusion.log.dir", logDirectory.toString());
            }
        }

        return props;
    }

    public void configurePluginClasspath() throws MojoExecutionException {
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

        configureServerClasspath();
    }

    public void configureServerClasspath() throws MojoExecutionException {

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

            if (!provided.isEmpty()) {
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

                serverClassLoader = new BlockingClassLoader(provided,
                        Collections.<String>emptySet(),
                        singleton("*"),
                        sharedClasses,
                        true);
                getLog().info("Plugin classpath set to : " + Arrays.toString(urls));
            }
        }
        catch (MalformedURLException e) {
            throw new MojoExecutionException("Invalid url", e);
        }
    }

    public boolean isPluginArtifact(Artifact artifact) {
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

    public void finishConfigurationBeforeStart(ServerConfig config) throws Exception {
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

    public void startDiffusion() throws MojoExecutionException {
        try {
            getLog().debug("Starting Diffusion Server ...");

            configureMonitor();

            printSystemProperties();

            if (server == null) {
                server = DiffusionServerWrapper.createServer(configureSystemProperties(), serverClassLoader);
            }

            finishConfigurationBeforeStart(server.getConfig());

            this.server.start();
            getLog().info("Started Diffusion Server");

            if (dumpOnStart) {
                //    getLog().info(this.server.dump());
            }
        }
        catch (MojoExecutionException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MojoExecutionException("Failed to start Diffusion", e);
        }
    }

    public void stopDiffusion() throws MojoExecutionException {
        if (server != null) {
            try {
                server.stop();
                getLog().info("Stopped Diffusion Server");
            }
            catch (Exception e) {
                throw new MojoExecutionException("Failure", e);
            }
        }
    }

    public EmbeddedDiffusion getServer() {
        return server;
    }

    public void configureMonitor() {
    }

    protected void printSystemProperties() {
        // print out which system properties were set up
        if (getLog().isDebugEnabled()) {
            if (systemProperties != null) {
                Iterator itor = systemProperties.getSystemProperties().iterator();
                while (itor.hasNext()) {
                    SystemProperty prop = (SystemProperty) itor.next();
                    getLog().debug("Property " + prop.getName() + "=" + prop.getValue() + " was " + (prop.isSet() ? "set" : "skipped"));
                }
            }
        }
    }

    public void setSystemPropertiesFile(File file) throws Exception {
        this.systemPropertiesFile = file;
        Properties properties = new Properties();
        try (InputStream propFile = new FileInputStream(systemPropertiesFile)) {
            properties.load(propFile);
        }
        if (this.systemProperties == null) {
            this.systemProperties = new SystemProperties();
        }

        for (Enumeration<?> keys = properties.keys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            if (!systemProperties.containsSystemProperty(key)) {
                SystemProperty prop = new SystemProperty();
                prop.setKey(key);
                prop.setValue(properties.getProperty(key));

                this.systemProperties.setSystemProperty(prop);
            }
        }
    }

    public void setSystemProperties(SystemProperties systemProperties) {
        if (this.systemProperties == null) {
            this.systemProperties = systemProperties;
        }
        else {
            for (SystemProperty prop : systemProperties.getSystemProperties()) {
                this.systemProperties.setSystemProperty(prop);
            }
        }
    }

    public boolean isExcluded(String goal) {
        if (excludedGoals == null || goal == null) {
            return false;
        }

        goal = goal.trim();
        if ("".equals(goal)) {
            return false;
        }

        boolean excluded = false;
        for (int i = 0; i < excludedGoals.length && !excluded; i++) {
            if (excludedGoals[i].equalsIgnoreCase(goal)) {
                excluded = true;
            }
        }

        return excluded;
    }

    /**
     * Check if a port is available. From SO.
     * @param port
     * @return
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
}
