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
	import flash.events.EventDispatcher;
	
	import org.osmf.events.MediaFactoryEvent;
	import org.osmf.media.MediaFactory;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.URLResource;

	/**
	 * This class is responsible for loading multiple plugins and passing along their configuration settings.
	 */ 
	public class PluginLoader extends EventDispatcher
	{
		// Public Interface
		//
		
		/**
		 * Constructor
		 *  
		 * @param pluginConfigurations
		 * @param mediaFactory
		 * 
		 */		
		public function PluginLoader(pluginConfigurations:Vector.<MediaResourceBase>, mediaFactory:MediaFactory)
		{
			this.pluginConfigurations = pluginConfigurations;
			this.mediaFactory = mediaFactory;
		}
		
		/**
		 * Loads all the external plugins. The playback of the media will start once all the plugins get loaded.
		 * If a plugin fails to load the Playwe will try to play the media file anyway. 
		 */ 
		public function loadPlugins():void
		{			
			if (pluginConfigurations.length > 0)
			{
				mediaFactory.addEventListener(MediaFactoryEvent.PLUGIN_LOAD, onPluginLoad);
				mediaFactory.addEventListener(MediaFactoryEvent.PLUGIN_LOAD_ERROR, onPluginLoadError);
				
				for each(var pluginConfiguration:MediaResourceBase in pluginConfigurations)
				{					
					mediaFactory.loadPlugin(pluginConfiguration);	
				}
			}
			else
			{
				dispatchEvent(new Event(Event.COMPLETE));
			}
		}
		
		// Internals
		//
		
		private function onPluginLoad(event:MediaFactoryEvent):void
		{
			loadedCount++;
			
			if (loadedCount == pluginConfigurations.length)
			{
				dispatchEvent(new Event(Event.COMPLETE));
			}
		}
		
		private function onPluginLoadError(event:MediaFactoryEvent):void
		{
			loadedCount++;
			
			if (loadedCount == pluginConfigurations.length)
			{
				dispatchEvent(new Event(Event.COMPLETE));
			}
		}
		
		private var pluginConfigurations:Vector.<MediaResourceBase>;
		private var mediaFactory:MediaFactory;
		private var loadedCount:int = 0;
	}
}