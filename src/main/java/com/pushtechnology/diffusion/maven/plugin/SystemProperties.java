/*
 * Copyright (C) 2015 Push Technology Ltd.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SystemProperties
 *
 * Map of name to SystemProperty.
 * 
 * When a SystemProperty instance is added, if it has not
 * been already set (eg via the command line java system property)
 * then it will be set.
 */
public class SystemProperties
{
    private final Map<String, SystemProperty> properties;
    private boolean force;

    public SystemProperties()
    {
        properties = new HashMap<>();
    }
    
    public void setForce (boolean force)
    {
        this.force = force;
    }
    
    public boolean getForce ()
    {
        return this.force;
    }


    public void setSystemProperty (SystemProperty prop)
    {
        properties.put(prop.getName(), prop);
        if (!force)
            prop.setIfNotSetAlready();
        else
            prop.setAnyway();
    }
    
    public SystemProperty getSystemProperty(String name)
    {
        return properties.get(name);
    }
    
    public boolean containsSystemProperty(String name)
    {
       return properties.containsKey(name); 
    }
    
    public List<SystemProperty> getSystemProperties ()
    {
        return new ArrayList<>(properties.values());
    }
}
