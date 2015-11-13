# diffusion-maven-plugin

The diffusion-maven-plugin is responsible for starting and stopping a Diffusion server for the purpose of running tests against that server.

## Goals overview

diffusion:start is the default goal invoked during the pre-integration-test phase for projects using this mojo. 
diffusion:stop is the default goal invoked during the post-integration-test phase for projects using this mojo.

## Usage

            <plugin>
                <artifactId>diffusion-maven-plugin</artifactId>
                <groupId>com.pushtechnology.diffusion.maven.plugin</groupId>
                <version>1.0</version>
                <configuration>
                    <systemPropeties>
                        <diffusion.home>${project.build.directory}/runtime</diffusion.home>
                    </systemPropeties>
                </configuration>
                <executions>
                    <execution>
                        <id>start-diffusion</id>
                        <phase>pre-integration-test</phase>
                        <goals>
                            <goal>start</goal>
                        </goals>
                        <configuration>
                            <serverStartTimeout>10000</serverStartTimeout>
                        </configuration>
                    </execution>
                    <execution>
                        <id>stop-diffusion</id>
                        <phase>post-integration-test</phase>
                        <goals>
                            <goal>stop</goal>
                        </goals>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                        <groupId>com.pushtechnology.diffusion</groupId>
                        <artifactId>diffusion</artifactId>
                        <version>${project.version}</version>
                        <scope>system</scope>
                        <systemPath>${project.build.directory}/runtime/lib/diffusion.jar</systemPath>
                    </dependency>
                </dependencies>
            </plugin>

Note that you should generally provide the location of your diffusion installation as a dependency and property.
If you do not then the value of DIFFUSION_HOME will be used if set.