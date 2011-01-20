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
	import flash.xml.XMLNode;
	
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.URLResource;
	import org.osmf.metadata.Metadata;

	/**
	 * Deserializes an XML file into a PlayerConfiguration object.
	 */ 
	public class ConfigurationXMLDeserializer
	{
		/**
		 * Constructs a <code>ConfigurationXMLDeserializer</code> instance and initialize it's properties.
		 * 
		 */
		public function deserialize(config:XML, playerConfiguration:PlayerConfiguration):void
		{
			var params:Object = new Object();
			addChildKeyValuePairs(config, params);
			
			// Retrieve children		
			var children:XMLList = config.children(); 
			for(var ci:uint = 0; ci < children.length(); ci++) {
				var child:XML = children[ci];
				var childName:String = child.name();
				if (childName == PLUGIN)
				{
					deserializePluginConfiguration(params, child);
				}	
				else if (childName == METADATA)
				{					
					addMetadataValues(child, params, ASSET_METADATA_PREFIX);
				}					
			}
			
			var flatDeserializer:ConfigurationFlashvarsDeserializer = new ConfigurationFlashvarsDeserializer();
			flatDeserializer.deserialize(params, playerConfiguration);
		}
		
		public function deserializePluginConfiguration(params:Object, pluginNode:XML):void
		{
			var pluginPrefix:String = PLUGIN + "_p";
			var pluginName:uint = 0;
			while (params.hasOwnProperty(pluginPrefix + pluginName))
			{
				pluginName++;
			}			
			var src:String = pluginNode.src || pluginNode.@src;
			params[pluginPrefix + pluginName] = src.toString();
			
			var pluginResource:MediaResourceBase = new URLResource(pluginNode.src || pluginNode.@src);
			var children:XMLList = pluginNode.children(); 
			for(var ci:uint = 0; ci < children.length(); ci++) {
				var child:XML = children[ci];
				var childName:String = child.name();
				if (childName == METADATA)
				{
					addMetadataValues(child, params, "p"+pluginName +"_");						
				}				
			}
		}
		
		// Internals
		//
		private function addMetadataValues(node:XML, params:Object, prefix:String = ""):void
		{
			var namespace:String = node.@id;			
			if (namespace.length > 0)
			{			
				var namespaceAlias:uint = 0;
				while (params.hasOwnProperty(prefix + NAMESPACE + "_n" + namespaceAlias))
				{
					namespaceAlias++;
				}
				params[prefix + NAMESPACE + "_n" + namespaceAlias] = namespace;
				prefix += "n" + namespaceAlias + "_";
			}		
			addChildKeyValuePairs(node, params, prefix);
		}	
		
		private function addChildKeyValuePairs(node:XML, params:Object, prefix:String = ""):void
		{			
			// Retrieve attributes
			var attributes:XMLList = node.attributes();			
			for(var ai:uint = 0; ai < attributes.length(); ai++) {
				var attributeName:String = attributes[ai].name();
				params[prefix+attributeName] = node.attribute(attributeName).toString();				
			}
			
			// Retrieve children		
			var children:XMLList = node.children(); 
			for(var ci:uint = 0; ci < children.length(); ci++) {
				var child:XML = children[ci];
				var childName:String = child.name();
				if (child.children().length() == 1)
				{
					params[prefix+childName] = child.children()[0].toXMLString();
				}
				else if (childName == PARAM)
				{
					params[prefix+child.@name] = child.@value.toXMLString();
				}
			}
		}
		
		private const METADATA:String = "metadata";
		private const PLUGIN:String = "plugin";
		private const PARAM:String = "param";
		private const NAMESPACE:String = "namespace";
		private const ASSET_METADATA_PREFIX:String = "src_";
	}
}