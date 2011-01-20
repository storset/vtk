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

package org.osmf.player.configuration
{
	import flash.events.Event;
	
	import flashx.textLayout.elements.Configuration;
	
	import mx.rpc.mxml.Concurrency;
	
	import org.flexunit.asserts.assertEquals;
	import org.flexunit.async.Async;

	public class TestConfigurationLoader
	{
		
		[Test(async, timeout="5000")]		
		public function testAssetMetadataLoading():void
		{
			var parameters:Object = 
				{
					src: "http://mediapm.edgesuite.net/edgeflash/public/debug/assets/smil/seas2.smil"
					,   src_namespace_akamai:"http://www.akamai.com/advancedstreamingplugin/1.0"
					,    src_akamai_akamaiMediaType: "akamai-hdn-multi-bitrate"
					,    plugin_AkamaiAdvancedStreamingPlugin: "http://localhost/test3/AkamaiAdvancedStreamingPlugin.swf"
				};
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			var configurationLoader:ConfigurationLoader = new ConfigurationLoader(new XMLFileLoader());
			Async.handleEvent(this, configurationLoader,
				Event.COMPLETE,
				onConfigurationLoaded, 4000);
			configurationLoader.load(parameters, configuration);
			function onConfigurationLoaded(event:Event, args:*):void
			{
				assertEquals(parameters.src, configuration.src);
				assertEquals(parameters.src_akamai_akamaiMediaType, configuration.assetMetadata["http://www.akamai.com/advancedstreamingplugin/1.0"].getValue("akamaiMediaType"));
			}
		}
		
		[Test(async, timeout="5000")]		
		public function testValid():void
		{
			var parameters:Object = 
			{
				configuration:"http://lolek.corp.adobe.com/strobe/assets/config/configuration.xml"
			};
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			var configurationLoader:ConfigurationLoader = new ConfigurationLoader(new XMLFileLoader());
			Async.handleEvent(this, configurationLoader,
				Event.COMPLETE,
				onConfigurationLoaded, 4000);
			configurationLoader.load(parameters, configuration);
			function onConfigurationLoaded(event:Event, args:*):void
			{
				assertEquals("http://mysite.com/my.flv", configuration.src);
			}
		}		
		
		[Test(async, timeout="5000")]		
		public function testInvalid():void
		{
			var parameters:Object = 
				{
					src:"http://mysite.com/my.flv",
					configuration:"invalid_configuration.xml"
				};
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			var configurationLoader:ConfigurationLoader = new ConfigurationLoader(new XMLFileLoader());
			Async.handleEvent(this, configurationLoader,
				Event.COMPLETE,
				onConfigurationLoaded, 4000);
			configurationLoader.load(parameters, configuration);
			function onConfigurationLoaded(event:Event, args:*):void
			{
				assertEquals("http://mysite.com/my.flv", configuration.src);
			}
		}
		
		[Test(async, timeout="5000")]		
		public function testInexisting():void
		{
			var parameters:Object = 
				{
					src:"http://mysite.com/my.flv",
					configuration:"inexisting_configuration.xml"
				};
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			var configurationLoader:ConfigurationLoader = new ConfigurationLoader(new XMLFileLoader());
			Async.handleEvent(this, configurationLoader,
				Event.COMPLETE,
				onConfigurationLoaded, 4000);
			configurationLoader.load(parameters, configuration);
			function onConfigurationLoaded(event:Event, args:*):void
			{
				assertEquals("http://mysite.com/my.flv", configuration.src);
			}
		}
	}
}