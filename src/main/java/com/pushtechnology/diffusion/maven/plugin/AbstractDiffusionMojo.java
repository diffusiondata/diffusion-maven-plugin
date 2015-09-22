//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package com.pushtechnology.diffusion.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.config.ConnectorConfig;
import com.pushtechnology.diffusion.api.config.LogConfig;
import com.pushtechnology.diffusion.api.config.ServerConfig;
import com.pushtechnology.diffusion.api.server.DiffusionServer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Common base class for most diffusion mojos.
 */
public abstract class AbstractDiffusionMojo extends AbstractMojo
{
    /**
     * Whether or not to include dependencies on the plugin's classpath with &lt;scope&gt;provided&lt;/scope&gt;
     * Use WITH CAUTION as you may wind up with duplicate jars/classes.
     * 
     * @parameter  default-value="false"
     */
    protected boolean useProvidedScope;
    
    /**
     * List of goals that are NOT to be used
     * 
     * @parameter
     */
    protected String[] excludedGoals;
    
    /**
     * File containing system properties to be set before execution
     * 
     * Note that these properties will NOT override System properties
     * that have been set on the command line, by the JVM, or directly 
     * in the POM via systemProperties. Optional.
     * 
     * @parameter property="diffusion.systemPropertiesFile"
     */
    protected File systemPropertiesFile;

    
    /**
     * System properties to set before execution. 
     * Note that these properties will NOT override System properties 
     * that have been set on the command line or by the JVM. They WILL 
     * override System properties that have been set via systemPropertiesFile.
     * Optional.
     * @parameter
     */
    protected SystemProperties systemProperties;

    /**
     * Directory to store Diffusion log files.
     *
     * @parameter property="${project.build.directory}/diffusion-server"
     */
    protected File logDirectory;
    
    
    /**
     * Comma separated list of a diffusion xml configuration files whose contents
     * will be applied before any plugin configuration. Optional.
     * 
     * 
     * @parameter alias="diffusionConfig"
     */
    protected String diffusionConfigDir;

    /**
     * Diffusion Server port
     *
     * @parameter default-value="8080"
     */
    protected int port;

    /**
     * Diffusion Server SSL port
     *
     * @parameter default-value="8443"
     */
    protected int sslPort;
    

    /**
     * Use the dump() facility of Diffusion to print out the server configuration to logging
     * 
     * @parameter property="dumponStart" default-value="false"
     */
    protected boolean dumpOnStart;
    
    
    /**  
     * Skip this mojo execution.
     * 
     * @parameter property="diffusion.skip" default-value="false"
     */
    protected boolean skip;

    /**
     * Number of milliseconds to wait for the server to start.
     *
     * @parameter default-value="60000"
     */
    protected long serverStartTimeout = 60000;

    /**
     * Whether to wait for server deployments to finish before declaring victory
     *
     * @parameter default-value="true"
     */
    protected boolean waitForDeployments = true;

    /**
     * The maven project.
     *
     * @parameter default-value="${project}"
     * @readonly
     */
    protected MavenProject project;

    
    /**
     * The artifacts for the project.
     * 
     * @parameter default-value="${project.artifacts}"
     * @readonly
     */
    protected Set projectArtifacts;
    
    
    /** 
     * @parameter default-value="${mojoExecution}" 
     * @readonly
     */
    protected org.apache.maven.plugin.MojoExecution execution;
    

    /**
     * The artifacts for the plugin itself.
     * 
     * @parameter default-value="${plugin.artifacts}"
     * @readonly
     */
    protected List pluginArtifacts;
    
    
    /**
     * A wrapper for the Server object
     * @parameter
     */
    protected DiffusionServer server;

    public abstract void checkPomConfiguration() throws MojoExecutionException;    
    
    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().info("Configuring Diffusion for project: " + this.project.getName());
        if (skip)
        {
            getLog().info("Skipping Diffusion start: diffusion.skip==true");
            return;
        }

        if (isExcluded(execution.getMojoDescriptor().getGoal()))
        {
            getLog().info("The goal \""+execution.getMojoDescriptor().getFullGoalName()+
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
        if (systemProperties != null)
        {
            Iterator itor = systemProperties.getSystemProperties().iterator();
            while (itor.hasNext())
            {
                SystemProperty prop = (SystemProperty)itor.next();
                props.put(prop.getName(), prop.getValue());
            }
        }

        // Fix up diffusion.home from environment if set.
        if (props.getProperty("diffusion.home") == null && System.getenv("DIFFUSION_HOME") != null) {
            props.setProperty("diffusion.home", System.getenv("DIFFUSION_HOME"));
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

    public void configurePluginClasspath() throws MojoExecutionException
    {  
        //if we are configured to include the provided dependencies on the plugin's classpath
        //(which mimics being on jetty's classpath vs being on the webapp's classpath), we first
        //try and filter out ones that will clash with jars that are plugin dependencies, then
        //create a new classloader that we setup in the parent chain.
        if (useProvidedScope)
        {
            try
            {
                List<URL> provided = new ArrayList<URL>();
                URL[] urls = null;
               
                for ( Iterator<Artifact> iter = projectArtifacts.iterator(); iter.hasNext(); )
                {                   
                    Artifact artifact = iter.next();
                    if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) && !isPluginArtifact(artifact))
                    {
                        provided.add(artifact.getFile().toURI().toURL());
                        if (getLog().isDebugEnabled()) { getLog().debug("Adding provided artifact: "+artifact);}
                    }
                }

                if (!provided.isEmpty())
                {
                    urls = new URL[provided.size()];
                    provided.toArray(urls);
                    URLClassLoader loader  = new URLClassLoader(urls, getClass().getClassLoader());
                    Thread.currentThread().setContextClassLoader(loader);
                    getLog().info("Plugin classpath augmented with <scope>provided</scope> dependencies: "+Arrays.toString(urls));
                }
            }
            catch (MalformedURLException e)
            {
                throw new MojoExecutionException("Invalid url", e);
            }
        }
    }
    
    public boolean isPluginArtifact(Artifact artifact)
    {
        if (pluginArtifacts == null || pluginArtifacts.isEmpty())
            return false;
        
        boolean isPluginArtifact = false;
        for (Iterator<Artifact> iter = pluginArtifacts.iterator(); iter.hasNext() && !isPluginArtifact; )
        {
            Artifact pluginArtifact = iter.next();
            if (getLog().isDebugEnabled()) { getLog().debug("Checking "+pluginArtifact);}
            if (pluginArtifact.getGroupId().equals(artifact.getGroupId()) && pluginArtifact.getArtifactId().equals(artifact.getArtifactId()))
                isPluginArtifact = true;
        }
        
        return isPluginArtifact;
    }

    public void finishConfigurationBeforeStart(ServerConfig config) throws Exception
    {
        ConnectorConfig connector = config.getConnector("Client Connector");
        if (connector != null) {
            connector.setPort(port);
        }
        connector = config.getConnector("HTTP Connector");
        if (connector != null) {
            connector.setPort(port);
        }
        connector = config.getConnector("SSL Connector");
        if (connector != null) {
            connector.setPort(sslPort);
        }
     }

    public void startDiffusion() throws MojoExecutionException
    {
        try
        {
            getLog().debug("Starting Diffusion Server ...");

            configureMonitor();
            
            printSystemProperties();

            if (server == null)
                server = new DiffusionServer(configureSystemProperties(), true);

            finishConfigurationBeforeStart(server.getConfig());

            this.server.start();
            getLog().info("Started Diffusion Server");

            if ( dumpOnStart )
            {
            //    getLog().info(this.server.dump());
            }
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Failure", e);
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

    public DiffusionServer getServer() {
        return server;
    }
    
    public void configureMonitor()
    { 
    }

    protected void printSystemProperties ()
    {
        // print out which system properties were set up
        if (getLog().isDebugEnabled())
        {
            if (systemProperties != null)
            {
                Iterator itor = systemProperties.getSystemProperties().iterator();
                while (itor.hasNext())
                {
                    SystemProperty prop = (SystemProperty)itor.next();
                    getLog().debug("Property "+prop.getName()+"="+prop.getValue()+" was "+ (prop.isSet() ? "set" : "skipped"));
                }
            }
        }
    }


    public void setSystemPropertiesFile(File file) throws Exception
    {
        this.systemPropertiesFile = file;
        Properties properties = new Properties();
        try (InputStream propFile = new FileInputStream(systemPropertiesFile))
        {
            properties.load(propFile);
        }
        if (this.systemProperties == null )
            this.systemProperties = new SystemProperties();
        
        for (Enumeration<?> keys = properties.keys(); keys.hasMoreElements();  )
        {
            String key = (String)keys.nextElement();
            if ( ! systemProperties.containsSystemProperty(key) )
            {
                SystemProperty prop = new SystemProperty();
                prop.setKey(key);
                prop.setValue(properties.getProperty(key));
                
                this.systemProperties.setSystemProperty(prop);
            }
        } 
    }
    
    public void setSystemProperties(SystemProperties systemProperties)
    {
        if (this.systemProperties == null)
            this.systemProperties = systemProperties;
        else
        {
            for (SystemProperty prop: systemProperties.getSystemProperties())
            {
                this.systemProperties.setSystemProperty(prop);
            }   
        }
    }

    public boolean isExcluded (String goal)
    {
        if (excludedGoals == null || goal == null)
            return false;
        
        goal = goal.trim();
        if ("".equals(goal))
            return false;
        
        boolean excluded = false;
        for (int i=0; i<excludedGoals.length && !excluded; i++)
        {
            if (excludedGoals[i].equalsIgnoreCase(goal))
                excluded = true;
        }
        
        return excluded;
    }
}
