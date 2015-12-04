# diffusion-maven-plugin

The _diffusion-maven-plugin_ enables you to start and stop a Diffusion server from within Maven for the purpose of running tests against that Diffusion server.

## Goals overview

* `diffusion:start` is the default goal invoked during the pre-integration-test phase for projects using this mojo. This goal starts a Diffusion server.
* `diffusion:stop` is the default goal invoked during the post-integration-test phase for projects using this mojo. This goal stops a Diffusion server.

## Dependencies

Use the _maven-failsafe-plugin_ to run your integration tests.

## Usage

The `diffusion-maven-plugin/src/main/examples/pom.xml` file shows how to include the _maven-failsafe-plugin_ and _diffusion-maven-plugin_ in your test suite POM file.

When you define the execution of the `diffusion:start` goal, you can configure a server start timeout. If the server takes longer than this time to start, the goal fails.

            <configuration>
                        <serverStartTimeout>10000</serverStartTimeout>
            </configuration>

Provide the location of your Diffusion server installation as a dependency and property. For example:

            <dependency>
                        <groupId>com.pushtechnology.diffusion</groupId>
                        <artifactId>diffusion</artifactId>
                        <version>${project.version}</version>
                        <scope>system</scope>
                        <systemPath>${env.DIFFUSION_HOME}/lib/diffusion.jar</systemPath>
            </dependency>

If you do not provide this as a dependency and property, the value of `DIFFUSION_HOME` will be used if set.

The `diffusion-maven-plugin/src/main/examples/` folder contains a single test, `DiffusionConnectIT.java`, that connects to a running Diffusion server with one of the default principals and passwords and subscribes to a topic. 

When you run `mvn clean install` in the `diffusion-maven-plugin/src/main/examples/` folder it starts a Diffusion server, runs the `DiffusionConnectIT.java` test against that server, and then stops the Diffusion server. You can use this example test to check that you have correctly set up the _diffusion-maven-plugin_
