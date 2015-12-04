# diffusion-maven-plugin

The diffusion-maven-plugin enables you to start and stop a Diffusion server from within Maven for the purpose of running tests against that Diffusion server.

## Goals overview

* diffusion:start is the default goal invoked during the pre-integration-test phase for projects using this mojo. This goal starts a Diffusion server.
* diffusion:stop is the default goal invoked during the post-integration-test phase for projects using this mojo. This goal stops a Diffusion server.

## Dependencies

Use the maven-failsafe-plugin to run your integration tests.

## Usage

The `diffusion-maven-plugin/src/main/examples/pom.xml` file shows how to include the maven-failsafe-plugin and diffusion-maven-plugin in your test suite POM file.

When you define the execution of the `diffusion:start` goal, you can configure a server start timeout. If the server takes longer than this time to start, the goal fails.
            <configuration>
                        <serverStartTimeout>10000</serverStartTimeout>
            </configuration>

Note: Provide the location of your Diffusion server installation as a dependency and property. For example:
            <dependency>
                        <groupId>com.pushtechnology.diffusion</groupId>
                        <artifactId>diffusion</artifactId>
                        <version>${project.version}</version>
                        <scope>system</scope>
                        <systemPath>${env.DIFFUSION_HOME}/lib/diffusion.jar</systemPath>
            </dependency>
If you do not provide this as a dependency and property, the value of DIFFUSION_HOME will be used if set.
