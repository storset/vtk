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
	import flash.events.EventDispatcher;
	import flash.events.IOErrorEvent;
	import flash.events.SecurityErrorEvent;
	import flash.net.URLLoader;
	import flash.net.URLRequest;
	
	[Event(name="complete", type="flash.events.Event")]
	
	/**
	 * Loads a configuration model from flashvars and external configuration files.
	 */ 
	public class ConfigurationLoader extends EventDispatcher
	{	
		public function ConfigurationLoader(loader:XMLFileLoader)
		{
			this.loader = loader;
		}
		
		public function load(parameters:Object, configuration:PlayerConfiguration):void
		{
			var configurationDeserializer:ConfigurationFlashvarsDeserializer = new ConfigurationFlashvarsDeserializer();
			// Parse configuration from the parameters passed on embedding
			// StrobeMediaPlayback.swf:
			if (parameters.hasOwnProperty("configuration"))
			{
				var xmlDeserializer:ConfigurationXMLDeserializer = new ConfigurationXMLDeserializer();
				loader.addEventListener(Event.COMPLETE, loadXMLConfiguration);
				loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, loadXMLConfiguration);
				loader.addEventListener(IOErrorEvent.IO_ERROR, loadXMLConfiguration);				
				function loadXMLConfiguration(event:Event):void
				{
					loader.removeEventListener(Event.COMPLETE, loadXMLConfiguration);
					loader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, loadXMLConfiguration);
					loader.removeEventListener(IOErrorEvent.IO_ERROR, loadXMLConfiguration);
					if (loader.xml != null)
					{
						xmlDeserializer.deserialize(loader.xml, configuration);
					}
					configurationDeserializer.deserialize(parameters, configuration);					
					dispatchEvent(new Event(Event.COMPLETE));	
				}
				loader.load(parameters.configuration);
			}
			else
			{			
				configurationDeserializer.deserialize(parameters, configuration);
				dispatchEvent(new Event(Event.COMPLETE));
			}	
		}	
		
		// Internals
		//
		private var loader:XMLFileLoader;
	}
}