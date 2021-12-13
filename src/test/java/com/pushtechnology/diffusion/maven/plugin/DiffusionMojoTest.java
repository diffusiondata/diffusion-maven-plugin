/*
 * Copyright 2014 Push Technology
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
 */

package com.pushtechnology.diffusion.maven.plugin;

import java.io.File;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Unit tests for {@link com.pushtechnology.diffusion.maven.plugin.AbstractDiffusionMojo}.
 *
 * @author Philip Aston
 * @author Andy Piper
 */
public class DiffusionMojoTest extends AbstractMojoTestCase {

    private static File testBaseDirectory =
            new File(getBasedir(), "target/mojo-test");

    private File buildDirectory;
    private File simplePom;

    @Mock
    private Artifact pluginArtifact;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        initMocks(this);

        String home = System.getenv("DIFFUSION_HOME");
        assertNotNull(home);
        when(pluginArtifact.getFile()).thenReturn(new File(home + "/lib/diffusion.jar"));

        testBaseDirectory.mkdirs();
        buildDirectory = File.createTempFile("build", "", testBaseDirectory);
        buildDirectory.delete();
        buildDirectory.mkdirs();

        simplePom = getTestFile("src/test/resources/unit/basic-test/pom.xml");
        assertTrue(simplePom.exists());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private DiffusionStartMojo getStartMojo(final File buildDirectory) throws Exception {
        final DiffusionStartMojo mojo = (DiffusionStartMojo) lookupMojo("start", simplePom);
        mojo.setPluginContext(new HashMap());

        setVariableValueToObject(mojo, "serverStartTimeout", 5000);
        setVariableValueToObject(mojo, "maxMessageSize", 32768);
        setVariableValueToObject(mojo, "execution",
                new DiffusionExecutionStub(null, "start", "boot"));
        setVariableValueToObject(mojo, "pluginArtifacts", Arrays.asList(new
                Artifact[]{pluginArtifact}));

        return mojo;
    }

    public void testBasicInvalid() throws Exception {

        final DiffusionStartMojo mojo = getStartMojo(buildDirectory);
        setVariableValueToObject(mojo, "project",
                new DiffusionProjectStub(buildDirectory, simplePom));
        setVariableValueToObject(mojo, "diffusionConfigDir",
                "some missing directory");
        try {
            mojo.execute();
            assertTrue(false);
        }
        catch (MojoExecutionException mee) {
            assertEquals(mee.getMessage(), "Invalid diffusionConfigDir");
        }
    }

    public void testBasicStart() throws Exception {

        final DiffusionStartMojo mojo = getStartMojo(buildDirectory);
        setVariableValueToObject(mojo, "project",
                new DiffusionProjectStub(buildDirectory, simplePom));

        mojo.execute();
        assertTrue(mojo.getServer().isStarted());
        // Clean up
        mojo.stopDiffusion();
    }

    public void testStartPortConflict() throws Exception {

        final DiffusionStartMojo mojo = getStartMojo(buildDirectory);
        setVariableValueToObject(mojo, "project",
                new DiffusionProjectStub(buildDirectory, simplePom));
        setVariableValueToObject(mojo, "port", 8080);
        ServerSocket ss = new ServerSocket(8080);

        try {
            mojo.execute();
            assertTrue(false);
        }
        catch (MojoExecutionException mee) {
            // should get here
        }
        ss.close();
        assertNull(mojo.getServer());
    }

    public void testStartSkip() throws Exception {

        final DiffusionStartMojo mojo = getStartMojo(buildDirectory);
        setVariableValueToObject(mojo, "project",
                new DiffusionProjectStub(buildDirectory, simplePom));
        setVariableValueToObject(mojo, "skip", true);

        mojo.execute();
        assertNull(mojo.getServer());
    }

    public void testBasicStop() throws Exception {

        final DiffusionStopMojo mojo = (DiffusionStopMojo) lookupMojo("stop", simplePom);
        final HashMap pluginContext = new HashMap();
        mojo.setPluginContext(pluginContext);
        final DiffusionStartMojo startmojo = getStartMojo(buildDirectory);
        MavenProject project = new DiffusionProjectStub(buildDirectory, simplePom);
        setVariableValueToObject(startmojo, "project", project);
        startmojo.execute();
        // Now stop it
        setVariableValueToObject(mojo, "execution",
                new DiffusionExecutionStub(null, "stop", "shutdown"));

        pluginContext.put("startedServerInstance", startmojo.getServer());
        setVariableValueToObject(mojo, "project", project);
        mojo.execute();
        assertTrue(mojo.getServer().isStopped());
    }
}
