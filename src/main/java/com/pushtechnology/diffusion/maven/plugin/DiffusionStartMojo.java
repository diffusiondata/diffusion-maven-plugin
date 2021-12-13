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

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

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
    @Override
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
}
