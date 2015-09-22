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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.pushtechnology.diffusion.api.LogDescription;
import com.pushtechnology.diffusion.api.config.ServerConfig;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * <p>
 * This goal is similar to the diffusion:run goal, EXCEPT that it is designed to be bound to an execution inside your pom, rather
 * than being run from the command line.
 * </p>
 *
 * @requiresDependencyResolution test
 * @execute phase="validate"
 * @description Runs diffusion directly from a maven project from a binding to an execution in your pom
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.VALIDATE)
public class DiffusionStartMojo extends AbstractDiffusionMojo {

    private LogReader reader;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        if (waitForDeployments) {
            long timeout = System.currentTimeMillis() + serverStartTimeout;
            Thread thr = new Thread(reader);
            thr.start();
            while (!reader.isStarted() && timeout > System.currentTimeMillis()) {
                synchronized (reader) {
                    try {
                        reader.wait(100);
                        timeout -= 100;
                    }
                    catch (InterruptedException e) {
                        throw new MojoExecutionException(e.getMessage(), e);
                    }
                }
            }
            if (!reader.isStarted()) {
                reader.stop();
                throw new MojoExecutionException("Server failed to start after " + serverStartTimeout / 1000 + "s");
            }
        }
        project.getProperties().put("startedServerInstance", server);
    }

    /**
     * Verify the configuration given in the pom.
     *
     * @see AbstractDiffusionMojo#checkPomConfiguration()
     */
    public void checkPomConfiguration() throws MojoExecutionException {
        if (diffusionConfigDir != null) {
            File configDir = new File(diffusionConfigDir);
            if (!configDir.exists() || !configDir.isDirectory()) {
                throw new MojoExecutionException("Invalid diffusionConfigDir");
            }
        }
    }

    @Override
    public void finishConfigurationBeforeStart(ServerConfig config) throws Exception {
        super.finishConfigurationBeforeStart(config);
        reader = new LogReader(logDirectory, config);
    }

    /**
     * A little bit of magic for figuring out the name of the server log file
     *
     * @param config
     * @return
     */
    private String getServerLogFileName(ServerConfig config) {
        LogDescription description = config.getLogging().getLog(config.getLogging().getServerLog());

        String tmp = description.getFilePattern().replaceAll("%s", "Server");

        if (config.getServerName() != null) {
            tmp = tmp.replaceAll("%n", config.getServerName());
        }

        final Date curDate = new Date(System.currentTimeMillis());
        final SimpleDateFormat format = new SimpleDateFormat(description.getDateFormat());

        format.setTimeZone(TimeZone.getDefault());

        return tmp.replaceAll("%d", format.format(curDate));
    }

    /**
     * Class to monitor the diffusion log file looking for the magic message "Diffusion finished deploying"
     */
    private class LogReader implements Runnable {
        private File logFile;
        private boolean stopped = false;
        private boolean started = false;

        LogReader(File logDirectory, ServerConfig config) throws IOException {
            logFile = new File(logDirectory, config.getLogging().getDefaultLogDirectory());
            logFile = new File(logFile, getServerLogFileName(config));
        }

        public synchronized boolean isStarted() {
            return started;
        }

        public synchronized boolean isStopped() {
            return stopped;
        }

        public synchronized void stop() {
            stopped = true;
        }

        public void run() {
            String line;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)));

                while (!isStarted() && !isStopped()) {
                    line = reader.readLine();
                    while (line == null) {
                        synchronized (this) {
                            wait(100);
                        }
                        line = reader.readLine();
                    }
                    if (line.toLowerCase().contains("diffusion finished deploying")) {
                        synchronized (this) {
                            this.started = true;
                            this.notifyAll();
                        }
                    }
                }
                synchronized (this) {
                    this.stopped = true;
                    this.notifyAll();
                }
                reader.close();
            }
            catch (final IOException ioe) {
                throw new AssertionError(ioe);
            }
            catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }
    }

}
