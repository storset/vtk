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
	import flash.events.TimerEvent;
	import flash.utils.Timer;
	
	import org.osmf.events.MediaFactoryEvent;
	import org.osmf.media.MediaFactory;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.PluginInfoResource;
	import org.osmf.media.URLResource;
	public class MockMediaFactory extends MediaFactory
	{
		public const REMOTE_INVALID_PLUGIN_SWF_URL:String = "Invalid.swf";
		public const INVALID_PLUGIN_CLASS:Class = org.osmf.player.plugins.InvalidPluginInfo;
		override public function loadPlugin(resource:MediaResourceBase):void
		{
			if (!resource is URLResource) 
			{
				var urlResource:URLResource = resource as URLResource;
				if (REMOTE_INVALID_PLUGIN_SWF_URL == urlResource.url)
				{
					var timer:Timer = new Timer(100, 1);
					timer.addEventListener(TimerEvent.TIMER, 
						function(event:Event):void 
						{
							dispatchEvent(new MediaFactoryEvent(MediaFactoryEvent.PLUGIN_LOAD_ERROR));
						}
					);
				}
				else
				{
					super.loadPlugin(resource);
				}
			}
			else if (resource is PluginInfoResource)
			{
				var pluginInfoResource:PluginInfoResource = resource as PluginInfoResource;
				if (pluginInfoResource.pluginInfo is INVALID_PLUGIN_CLASS)
				{
					dispatchEvent(new MediaFactoryEvent(MediaFactoryEvent.PLUGIN_LOAD_ERROR));					
				}
				else
				{
					super.loadPlugin(resource);
				}
			}
		}
	}
}