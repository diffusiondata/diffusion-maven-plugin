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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Stops a running instance of Diffusion.
 */
@Mojo(name = "stop")
public class DiffusionStopMojo extends AbstractDiffusionMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping Diffusion stop: diffusion.skip==true");
            return;
        }

        stopDiffusion();
    }
}
