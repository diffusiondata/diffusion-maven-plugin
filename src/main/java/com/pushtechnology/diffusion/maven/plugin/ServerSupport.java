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

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pushtechnology.diffusion.api.server.DiffusionServer;

/**
 * ServerSupport
 *
 * Helps configure the Server instance.
 * 
 */
public class ServerSupport
{
    

    /**
     * Configure at least one connector for the server
     * 
     * @param server the server
     * @param connector the connector

    public static void configureConnectors (Server server, Connector connector)
    {
        if (server == null)
            throw new IllegalArgumentException("Server is null");
        
        //if a connector is provided, use it
        if (connector != null)
        {
            server.addConnector(connector);
            return;
        }
        
        

        // if the user hasn't configured the connectors in a jetty.xml file so use a default one
        Connector[] connectors = server.getConnectors();
        if (connectors == null || connectors.length == 0)
        {
            //Make a new default connector
            MavenServerConnector tmp = new MavenServerConnector();
            //use any jetty.http.port settings provided
            String port = System.getProperty(MavenServerConnector.PORT_SYSPROPERTY, System.getProperty("jetty.port", MavenServerConnector.DEFAULT_PORT_STR));
            tmp.setPort(Integer.parseInt(port.trim()));
            tmp.setServer(server);
            server.setConnectors(new Connector[] {tmp});
        }
    }
     */



    /**
     * Apply xml files to server startup, passing in ourselves as the 
     * "Server" instance.
     * 
     * @param server the server to apply the xml to
     * @param files the list of xml files
     * @return the Server implementation, after the xml is applied
     * @throws Exception if unable to apply the xml configuration

    public static DiffusionServer applyXmlConfigurations (DiffusionServer server, List<File> files)
    throws Exception
    {
        if (files == null || files.isEmpty())
            return server;

        Map<String,Object> lastMap = new HashMap<String,Object>();
        
        if (server != null)
            lastMap.put("Server", server);
     

        for ( File xmlFile : files )
        {
            if (PluginLog.getLog() != null)
                PluginLog.getLog().info( "Configuring Diffusion from xml configuration file = " + xmlFile.getCanonicalPath() );


            XmlConfiguration xmlConfiguration = new XmlConfiguration(Resource.toURL(xmlFile));

            //chain ids from one config file to another
            if (lastMap != null)
                xmlConfiguration.getIdMap().putAll(lastMap); 

            //Set the system properties each time in case the config file set a new one
            Enumeration<?> ensysprop = System.getProperties().propertyNames();
            while (ensysprop.hasMoreElements())
            {
                String name = (String)ensysprop.nextElement();
                xmlConfiguration.getProperties().put(name,System.getProperty(name));
            }
            xmlConfiguration.configure(); 
            lastMap = xmlConfiguration.getIdMap();
        }
        
        return (Server)lastMap.get("Server");
    }
     */

}
