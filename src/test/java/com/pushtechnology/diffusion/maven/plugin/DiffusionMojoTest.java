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

import static java.util.Arrays.asList;
import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * Unit tests for {@link DiffusionMojoTest}.
 *
 * @author Philip Aston
 * @author Andy Piper
 */
public class DiffusionMojoTest extends AbstractMojoTestCase {

    private static File testBaseDirectory =
        new File(getBasedir(), "target/mojo-test");

    private File buildDirectory;
    private File simplePom;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

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

    private DiffusionStartMojo getMojo(final File buildDirectory) throws Exception {
        final DiffusionStartMojo mojo = (DiffusionStartMojo) lookupMojo("start", simplePom);

//        setVariableValueToObject(mojo, "buildDirectory",
//            buildDirectory.getAbsolutePath());
//        setVariableValueToObject(mojo, "finalName", "mydar");
//        setVariableValueToObject(mojo, "outputDirectory",
//            new File("some missing directory"));
        setVariableValueToObject(mojo, "diffusionConfigDir",
            "some missing directory");
        setVariableValueToObject(mojo, "execution",
                new DiffusionExecutionStub(null, "start", "boot"));

        return mojo;
    }

    public void testBasic() throws Exception {

        final DiffusionStartMojo mojo = getMojo(buildDirectory);
        setVariableValueToObject(mojo, "project",
            new DiffusionProjectStub(buildDirectory, simplePom));

        mojo.execute();
    }

            /*
    public void testDependencies() throws Exception {

        final DiffusionStartMojo mojo = getMojo(buildDirectory);

        final DiffusionProjectStub project =
            new DiffusionProjectStub(buildDirectory, simplePom);
        setVariableValueToObject(mojo, "project", project);

        final Artifact missingFile =
            createArtifact("group", "a5", "jar", false, SCOPE_COMPILE);
        when(missingFile.getFile()).thenReturn(null);

        final Set<Artifact> dependencyArtifacts = asSet(
            createArtifact("group", "a1", "jar", true, SCOPE_COMPILE),
            createArtifact("group", "a2", "jar", false, SCOPE_COMPILE),
            createArtifact("group", "a3", "jar", false, SCOPE_TEST),
            createArtifact("com.pushtechnology", "diffusion-api", "jar",
                false, SCOPE_COMPILE),
            createArtifact("group", "a4", "jar", false, SCOPE_COMPILE),
            missingFile,
            createArtifact("group", "a6", "whatever", false, SCOPE_COMPILE)
            );

        project.setDependencyArtifacts(dependencyArtifacts);

        mojo.execute();
    }

    public void testResources() throws Exception {

        final DiffusionStartMojo mojo = getMojo(buildDirectory);

        final DiffusionProjectStub project =
            new DiffusionProjectStub(buildDirectory, simplePom);
        setVariableValueToObject(mojo, "project", project);

        final File d = new File(buildDirectory, "resources");
        d.mkdir();

        setVariableValueToObject(mojo, "diffusionResourceDirectory", d);

        new File(d, "etc").mkdir();
        new File(d, "html").mkdir();
        new File(d, "foo").mkdir();

        new File(d, "etc/Publishers.xml").createNewFile();
        new File(d, "foo/ignored.txt").createNewFile();
        new File(d, "html/hello.html").createNewFile();

        mojo.execute();
    }

    public void testOutput() throws Exception {

        final DiffusionStartMojo mojo = getMojo(buildDirectory);

        final DiffusionProjectStub project =
            new DiffusionProjectStub(buildDirectory, simplePom);
        setVariableValueToObject(mojo, "project", project);

        final File d = new File(buildDirectory, "output");
        d.mkdir();

        setVariableValueToObject(mojo, "outputDirectory", d);
        setVariableValueToObject(mojo, "outputExcludes",
            new String[] { "foo/**" });

        new File(d, "x/y").mkdirs();
        new File(d, "foo").mkdir();

        new File(d, "foo/ignored.txt").createNewFile();
        new File(d, "x/y/z").createNewFile();

        mojo.execute();
    }
    */

    private Artifact createArtifact(
        final String groupId,
        final String artifactId,
        final String type,
        final boolean optional,
        final String scope) throws IOException {

        final Artifact result = mock(Artifact.class);
        when(result.isOptional()).thenReturn(optional);
        when(result.getScope()).thenReturn(scope);
        when(result.getGroupId()).thenReturn(groupId);
        when(result.getArtifactId()).thenReturn(artifactId);
        when(result.getType()).thenReturn(type);

        final File f =
            new File(buildDirectory,
                String.format("%s-%s.%s",
                    groupId, artifactId, type));
        f.createNewFile();

        when(result.getFile()).thenReturn(f);

        return result;
    }

    public static final <T> Set<T> asSet(final T... a) {
        return asSet(asList(a));
    }

    public static final <T> Set<T> asSet(final Collection<T> list) {
        return new HashSet<T>(list);
    }
}
