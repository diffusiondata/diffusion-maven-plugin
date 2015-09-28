//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  Copyright (c) 2015 Push Technology Ltd.
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

import com.pushtechnology.diffusion.api.server.Diffusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * DiffusionStopMojo - stops a running instance of Diffusion.
 *
 * @description Stops diffusion
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.VALIDATE)
public class DiffusionStopMojo extends AbstractDiffusionMojo {
    @Override
    public void checkPomConfiguration() throws MojoExecutionException {

    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        server = (Diffusion) project.getProperties().get("startedServerInstance");
        stopDiffusion();
    }
}
