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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;


/**
 *  <p>
 *  This goal is similar to the diffusion:run goal, EXCEPT that it is designed to be bound to an execution inside your pom, rather
 *  than being run from the command line. 
 *  </p>
 *
 * @requiresDependencyResolution test
 * @execute phase="validate"
 * @description Runs diffusion directly from a maven project from a binding to an execution in your pom
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.VALIDATE)
public class DiffusionStartMojo extends AbstractDiffusionMojo
{

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        //ensure that starting jetty won't hold up the thread
        super.execute();
    }

    /**
     * Verify the configuration given in the pom.
     *
     * @see AbstractDiffusionMojo#checkPomConfiguration()
     */
    public void checkPomConfiguration () throws MojoExecutionException {
        if (diffusionConfigDir != null) {
            File configDir = new File(diffusionConfigDir);
            if (!configDir.exists() || !configDir.isDirectory()) {
                throw new MojoExecutionException("Invalid diffusionConfigDir");
            }
        }
    }

    @Override
    public void finishConfigurationBeforeStart() throws Exception
    {
        super.finishConfigurationBeforeStart();
        //server.setStopAtShutdown(false); //as we will normally be stopped with a cntrl-c, ensure server stopped
    }
    
}
