/*****************************************************
 *  
 *  Copyright 2010 Adobe Systems Incorporated.  All Rights Reserved.
 *  
 *****************************************************
 *  The contents of this file are subject to the Mozilla Public License
 *  Version 1.1 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *   
 *  Software distributed under the License is distributed on an "AS IS"
 *  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 *  License for the specific language governing rights and limitations
 *  under the License.
 *   
 *  
 *  The Initial Developer of the Original Code is Adobe Systems Incorporated.
 *  Portions created by Adobe Systems Incorporated are Copyright (C) 2010 Adobe Systems 
 *  Incorporated. All Rights Reserved. 
 *  
 *****************************************************/

package org.osmf.player.plugins
{
	import flash.events.Event;
	
	import org.flexunit.asserts.assertEquals;
	import org.flexunit.asserts.assertNotNull;
	import org.flexunit.asserts.assertNull;
	import org.flexunit.asserts.assertTrue;
	import org.flexunit.asserts.fail;
	import org.osmf.events.MediaFactoryEvent;
	import org.osmf.media.MediaElement;
	import org.osmf.media.MediaFactory;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.PluginInfoResource;
	import org.osmf.media.URLResource;
	import org.osmf.metadata.Metadata;
	import org.osmf.player.configuration.ConfigurationFlashvarsDeserializer;

	public class TestPluginLoader
	{
		[Test] 
		public function testEchoPlugin():void
		{	
			var factory:MediaFactory;
			factory = new MediaFactory();
			var pluginResource:MediaResourceBase = new PluginInfoResource(new ValidPluginInfo());			
			pluginResource.addMetadataValue(PLUGIN_NAMESPACE, {
				videoName: "Video Name",
				description: "Video Description"
			});			
			factory.addEventListener(MediaFactoryEvent.PLUGIN_LOAD, onPluginLoad);
			function onPluginLoad(event:Event, param2:*=null):void
			{			
				trace(event.type);
				var mediaElement:MediaElement = factory.createMediaElement(new URLResource("http://my.com/my.flv"));
				var metadata:Metadata = mediaElement.metadata.getValue(PLUGIN_NAMESPACE) as Metadata;
				var pluginMetadata:Object = metadata.getValue(PLUGIN_NAMESPACE);			
				assertEquals("invalid videoName is set", "Video Name", pluginMetadata.videoName);
				assertEquals("invalid description is set", "Video Description", pluginMetadata.description);		
			}	
			factory.loadPlugin(pluginResource);			
		}
		
		[Test] 
		public function testMultipleNamespaces():void
		{	
			var factory:MediaFactory;
			factory = new MediaFactory();
			var parameters:Object = {
				plugin_echo:"org.osmf.player.plugins.ValidPluginInfo",
				echo_namespace: PLUGIN_NAMESPACE,
				echo_namespace_ns2: PLUGIN_NAMESPACE2,
				echo_videoName: "Video Name",
				echo_description: "Video Description",
				
				echo_ns2_videoName: "Video Name 2",
				echo_ns2_description: "Video Description 2"
			};			
			var configurationFlashvarsDeserializer:ConfigurationFlashvarsDeserializer = new ConfigurationFlashvarsDeserializer();
			var pluginConfigurations:Vector.<MediaResourceBase> = new Vector.<MediaResourceBase>(); 
			configurationFlashvarsDeserializer.deserializePluginConfigurations(parameters, pluginConfigurations);
			
			var pluginLoader:PluginLoader = new PluginLoader(pluginConfigurations, factory);	
			factory.addEventListener(MediaFactoryEvent.PLUGIN_LOAD, onPluginLoad);	
			function onPluginLoad(event:Event, param2:*=null):void
			{	
				var mediaElement:MediaElement = factory.createMediaElement(new URLResource("http://my.com/my.flv"));
				var metadata:Metadata = mediaElement.metadata.getValue(PLUGIN_NAMESPACE) as Metadata;
				var pluginMetadata:Metadata = metadata.getValue(PLUGIN_NAMESPACE);	
				assertNotNull(pluginMetadata);
				assertEquals("invalid videoName is set", parameters.echo_videoName, pluginMetadata.getValue("videoName"));
				assertEquals("invalid description is set", parameters.echo_description, pluginMetadata.getValue("description"));	
				
				var pluginMetadata2:Metadata = metadata.getValue(PLUGIN_NAMESPACE2);	
				assertNotNull(pluginMetadata2);
				assertEquals("invalid videoName is set", parameters.echo_ns2_videoName, pluginMetadata2.getValue("videoName"));
				assertEquals("invalid description is set", parameters.echo_ns2_description, pluginMetadata2.getValue("description"));
			}	
			pluginLoader.loadPlugins();
		}
		
		[Test] 
		public function integrationOneBadOneGoodTest():void
		{				
			var factory:MediaFactory;
			factory = new MockMediaFactory();
			
			var pluginConfigurations:Vector.<MediaResourceBase> = new Vector.<MediaResourceBase>();
			pluginConfigurations[0] = new PluginInfoResource(new InvalidPluginInfo());
			pluginConfigurations[1] = new PluginInfoResource(new ValidPluginInfo());
			var pluginLoader:PluginLoader = new PluginLoader(pluginConfigurations, factory);
			
			var loaded:Boolean = false;			
			pluginLoader.addEventListener(Event.COMPLETE, 
				function(event:Event):void
				{
					loaded = true;
				}
			);			
			pluginLoader.loadPlugins();
			assertTrue(loaded);			
		}
		
		[Test] 
		public function integrationOneGoodOneBadTest():void
		{				
			var factory:MediaFactory;
			factory = new MockMediaFactory();
			
			var pluginConfigurations:Vector.<MediaResourceBase> = new Vector.<MediaResourceBase>();
			pluginConfigurations[0] = new PluginInfoResource(new ValidPluginInfo());
			pluginConfigurations[1] = new PluginInfoResource(new InvalidPluginInfo());
			var pluginLoader:PluginLoader = new PluginLoader(pluginConfigurations, factory);
			
			var loaded:Boolean = false;			
			pluginLoader.addEventListener(Event.COMPLETE, 
				function(event:Event):void
				{
					loaded = true;
				}
			);			
			pluginLoader.loadPlugins();
			assertTrue(loaded);					
		}
		
	
		private static const PLUGIN_NAMESPACE:String = "http://www.osmf.org/plugin/metadata/1.0";
		private static const PLUGIN_NAMESPACE2:String = "http://www.osmf.org/plugin/metadata/2.0";
		private static const ASYNC_DELAY:uint = 9000;
	}
}



