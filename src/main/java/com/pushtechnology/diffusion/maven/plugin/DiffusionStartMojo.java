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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.diffusion.api.LogDescription;
import com.pushtechnology.diffusion.api.config.ServerConfig;
import com.pushtechnology.diffusion.api.server.EmbeddedDiffusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * <p>
 * This goal is similar to the diffusion:run goal, except that it is designed to be bound to an 
 * execution inside your POM, rather than being run from the command line.
 * </p>
 *
 * @requiresDependencyResolution test
 * @execute phase="pre-integration-test"
 * @description Runs Diffusion directly from a Maven project from a binding to an execution in your POM
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class DiffusionStartMojo extends AbstractDiffusionMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        if (skip) {
            getLog().info("Skipping Diffusion start: diffusion.skip==true");
            return;
        }
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
     * Verify the configuration given in the POM.
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
    }

    /**
     * Figures out the name of the Diffusion server log file
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
     * Class to monitor the Diffusion log file looking for the message "Diffusion finished deploying"
     * Not used currently. Would be useful for older versions of Diffusion.
     */
    private class LogReader implements Runnable {
        private File logFile;
        private boolean stopped = false;
        private boolean started = false;

        LogReader(File logDirectory, ServerConfig config) throws IOException {
            logFile = new File(logDirectory, config.getLogging().getDefaultLogDirectory());
            logFile = new File(logFile, getServerLogFileName(config));
            // On older versions of Diffusion we cannot control where the log files go
            if (!logFile.exists() && diffusionHome != null) {
                File alt = new File(diffusionHome, config.getLogging().getDefaultLogDirectory());
                alt = new File(alt, getServerLogFileName(config));
                if (alt.exists()) {
                    logFile = alt;
                }
            }
            getLog().info("Monitoring log file at " + logFile.toString());
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
