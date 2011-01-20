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
	import flash.external.ExternalInterface;
	
	import org.osmf.media.MediaElement;
	import org.osmf.media.MediaFactoryItem;
	import org.osmf.media.MediaFactoryItemType;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.PluginInfo;
	import org.osmf.media.URLResource;
	import org.osmf.metadata.Metadata;
	import org.osmf.net.NetLoader;

	/**
	 * This plugin simply echos back all the metadata it receives as metadata on newly created <code>MediaElement</code>s
	 */ 
	public class ConfigurationEchoPluginInfo extends PluginInfo
	{
		public static const PLUGIN_METADATA_NAMESPACE:String = "http://www.osmf.org/plugin/metadata/1.0";

		
		public function ConfigurationEchoPluginInfo()
		{
			var items:Vector.<MediaFactoryItem> = new Vector.<MediaFactoryItem>();
			
			var item:MediaFactoryItem = new MediaFactoryItem("org.osmf.player.configuration",
				canHandleResource,
				createMediaElement,
				MediaFactoryItemType.STANDARD);
			items.push(item);
			super(items);
		}	
		
		/**
		 * @inheritDoc
		 **/
		override public function initializePlugin(resource:MediaResourceBase):void
		{
			pluginMetadata = new Metadata();
			for each(var ns:String in resource.metadataNamespaceURLs)
			{				
				var metadata:Object = resource.getMetadataValue(ns);
				pluginMetadata.addValue(ns, metadata);
			}
			echoResourceMetadata("plugin metadata:", resource);
		}
		
		/**
		 * Handles everything
		 */ 
		private function canHandleResource(resource:MediaResourceBase):Boolean
		{		
			echoResourceMetadata("canHandleResource:", resource);
			return true;
		}
		
		/**
		 * Creates a new MediaElement and sets on it the plugin metadata
		 */ 
		private function createMediaElement():MediaElement
		{
			var mediaElement:MediaElement = new MediaElement();
			mediaElement.addMetadata(PLUGIN_METADATA_NAMESPACE, pluginMetadata);
			
			return mediaElement;
		}
		
		private function echoResourceMetadata(type:String, resource:MediaResourceBase):void
		{	
			var alert:Boolean = false;
			var message:String = type + "\n";
			if (resource.metadataNamespaceURLs.length > 0)
			{
				for each(var ns:String in resource.metadataNamespaceURLs)
				{	
					var metadata:Object = resource.getMetadataValue(ns);
					if (metadata!=null)
					{
						if (metadata is String)
						{
							message += ns + ":" + metadata + "\n";
							trace(ns + ":" + metadata);	
							if (ns == "alert")
							{
								alert = true;
							}
						}
						else
						{
							message += "\nnamespace=" + ns + "\n";
							trace(message, ns);
							for (var key:String in metadata)
							{
								message += ns + ":" + key + ":" + metadata[key] + "\n";
								trace(ns + ":" + key + ":" + metadata[key]);
								if (key == "alert")
								{
									alert = true;
								}
							}	
						}					
					}	
					message += "\n";
				}
				if (alert && message != null)
				{
					ExternalInterface.call("alert", message);
				}
			}
		}
		private var pluginMetadata:Metadata;
		
	}
}