package com.pushtechnology.diffusion.maven.plugin;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * @author andy
 */
public class DiffusionExecutionStub extends MojoExecution {
    public DiffusionExecutionStub(Plugin plugin, String goal, String executionId) {
        super(plugin, goal, executionId);
    }

    @Override
    public MojoDescriptor getMojoDescriptor() {
        return new MojoDescriptor() {
            @Override
            public String getGoal() {
                return "start";
            }
        };
    }
}
