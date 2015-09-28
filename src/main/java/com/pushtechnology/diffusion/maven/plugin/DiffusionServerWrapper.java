/*
 * Copyright (C) 2015 Push Technology Ltd.
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

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import com.pushtechnology.diffusion.api.server.Diffusion;
import com.pushtechnology.diffusion.api.server.DiffusionServer;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author andy
 */
public class DiffusionServerWrapper {

    public static Diffusion createServer(Properties properties, ClassLoader serverClassLoader) throws MojoExecutionException {
        try {
            return (Diffusion) Class.forName(
                    DiffusionServer.class.getName(),
                    true,
                    serverClassLoader)
                    .getConstructor(Properties.class, boolean.class)
                    .newInstance(properties, true);
        }
        catch (InstantiationException |
                IllegalAccessException |
                InvocationTargetException |
                NoSuchMethodException |
                ClassNotFoundException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }
}
