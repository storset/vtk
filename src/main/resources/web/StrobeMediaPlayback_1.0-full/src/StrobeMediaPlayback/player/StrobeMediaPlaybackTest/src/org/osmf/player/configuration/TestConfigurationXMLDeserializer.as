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
	import org.flexunit.asserts.assertEquals;
	import org.flexunit.asserts.assertNull;
	import org.osmf.media.URLResource;

	public class TestConfigurationXMLDeserializer
	{
		[Test]
		public function testSrc():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var config:XML =
				<config>
					<src>{url}</src>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
		}
		
		[Test]
		public function testSrcAttribute():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var config:XML =
				<config src={url}>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
		}		
		
		
		[Test]
		public function testPlugin():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var pluginUrl:String = "http://mysite.com/myplugin.swf";
			var config:XML =
				<config>
					<src>{url}</src>
					<plugin src={pluginUrl}>						
						<metadata id="NAMESPACE_D"> 
							<param name="account" value="gfgdfg"/>
							<param name="trackingServer" value="corp1.d1.sc.omtrdc.net"/>
						</metadata>
					</plugin>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
			assertEquals(1, configuration.pluginConfigurations.length);
			assertEquals(pluginUrl, (configuration.pluginConfigurations[0] as URLResource).url);
			assertEquals("gfgdfg", configuration.pluginConfigurations[0].getMetadataValue("NAMESPACE_D").getValue("account"));
			assertEquals("corp1.d1.sc.omtrdc.net", configuration.pluginConfigurations[0].getMetadataValue("NAMESPACE_D").getValue("trackingServer"));
		}
		
		[Test]
		public function testPluginInvalidMetadataElement():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var pluginUrl:String = "http://mysite.com/myplugin.swf";
			var config:XML =
				<config>
					<src>{url}</src>
					<plugin src={pluginUrl}>						
						<invalid id="NAMESPACE_D"> 
							<param name="account" value="gfgdfg"/>
							<param name="trackingServer" value="corp1.d1.sc.omtrdc.net"/>
						</invalid>
					</plugin>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
			assertEquals(1, configuration.pluginConfigurations.length);
			assertEquals(pluginUrl, (configuration.pluginConfigurations[0] as URLResource).url);
			assertNull(configuration.pluginConfigurations[0].getMetadataValue("NAMESPACE_D"));
		}
		
		[Test]
		public function testPluginMetadataNoNamespace():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var pluginUrl:String = "http://mysite.com/myplugin.swf";
			var config:XML =
				<config>
					<src>{url}</src>
					<plugin src={pluginUrl}>						
						<metadata> 
							<param name="account" value="gfgdfg"/>
							<param name="trackingServer" value="corp1.d1.sc.omtrdc.net"/>
						</metadata>
					</plugin>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
			assertEquals(1, configuration.pluginConfigurations.length);
			assertEquals(pluginUrl, (configuration.pluginConfigurations[0] as URLResource).url);
			assertEquals("gfgdfg", configuration.pluginConfigurations[0].getMetadataValue("account"));
			assertEquals("corp1.d1.sc.omtrdc.net", configuration.pluginConfigurations[0].getMetadataValue("trackingServer"));
		}
		
		[Test]
		public function testAssetMetadata():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var pluginUrl:String = "http://mysite.com/myplugin.swf";
			var config:XML =
				<config>
					<src>{url}</src>
					<metadata id="NAMESPACE_D"> 
						<param name="account" value="gfgdfg"/>
						<param name="trackingServer" value="corp1.d1.sc.omtrdc.net"/>                        
					</metadata>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
			assertEquals("gfgdfg", configuration.assetMetadata["NAMESPACE_D"].getValue("account"));
			assertEquals("corp1.d1.sc.omtrdc.net", configuration.assetMetadata["NAMESPACE_D"].getValue("trackingServer"));
		}
		
		[Test]
		public function testAssetMetadataNoNamespace():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var pluginUrl:String = "http://mysite.com/myplugin.swf";
			var config:XML =
				<config>
					<src>{url}</src>
					<metadata> 
						<param name="account" value="gfgdfg"/>
						<param name="trackingServer" value="corp1.d1.sc.omtrdc.net"/>                        
					</metadata>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
			assertEquals("gfgdfg", configuration.assetMetadata.account);
			assertEquals("corp1.d1.sc.omtrdc.net", configuration.assetMetadata.trackingServer);
		}		
		
		[Test]
		public function testAssetMultipleMetadataNamespaces():void			
		{
			var url:String = "http://mysite.com/my.flv";
			var pluginUrl:String = "http://mysite.com/myplugin.swf";
			var config:XML =
				<config>
					<src>{url}</src>
					<metadata id="NAMESPACE_ONE"> 
						<param name="account" value="one"/>
						<param name="trackingServer" value="one.net"/>                        
					</metadata>
					<metadata id="NAMESPACE_TWO"> 
						<param name="account" value="two"/>
						<param name="trackingServer" value="two.net"/>                        
					</metadata>
				</config>;
			var deserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
			var configuration:PlayerConfiguration = new PlayerConfiguration();
			deserializer.deserialize(config, configuration);
			assertEquals(url, configuration.src);
			assertEquals("one", configuration.assetMetadata["NAMESPACE_ONE"].getValue("account"));
			assertEquals("one.net", configuration.assetMetadata["NAMESPACE_ONE"]..getValue("trackingServer"));
			assertEquals("two", configuration.assetMetadata["NAMESPACE_TWO"]..getValue("account"));
			assertEquals("two.net", configuration.assetMetadata["NAMESPACE_TWO"]..getValue("trackingServer"));
		}
	}
}