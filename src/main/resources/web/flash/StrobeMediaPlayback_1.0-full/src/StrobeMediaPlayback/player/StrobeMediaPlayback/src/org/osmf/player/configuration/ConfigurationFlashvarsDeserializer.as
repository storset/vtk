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
	import flash.system.ApplicationDomain;
	import flash.utils.*;
	import flash.utils.Dictionary;
	
	import org.osmf.player.chrome.configuration.ConfigurationUtils;
	import org.osmf.layout.ScaleMode;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.PluginInfoResource;
	import org.osmf.media.URLResource;
	import org.osmf.metadata.Metadata;
	import org.osmf.player.metadata.StrobeDynamicMetadata;
	import org.osmf.utils.URL;

	/**
	 * ConfigurationFlashvarsDeserializer is currently responsible
	 * for deserializing FlashVars, validating the input values and storing the external configuration values.
	 * 
	 * We will probably split this class into specialized classes responsible for 
	 * Deserialization and Validation
	 *  
	 */ 
	public class ConfigurationFlashvarsDeserializer
	{
		/**
		 * Constructs a <code>PlayerConfiguration</code> instance and initialize it's properties.
		 * 
		 * This method also validates all the properties and this code will probably be moved to a 
		 * specialized class as soon as this need arises.
		 */
		public function deserialize(parameters:Object, playerConfiguration:PlayerConfiguration):void
		{		
			// WORKARROUND: for FM-950 - to be removed once this is fixed.
			if (parameters.hasOwnProperty("src") && parameters.src != null)
			{
				// Handle the special case where the user is tring to connect to a dev server running on 
				//	the same machine as the client with a url like this: "rtmp:/sudoku/room1"
				var oneSlashRegExp:RegExp = /^(rtmp|rtmp[tse]|rtmpte|rtmfp)(:\/[^\/])/i;
				var oneSlashResult:Array = parameters.src.match(oneSlashRegExp);
				var tempUrl:String = parameters.src;
				
				if (oneSlashResult != null)
				{
					tempUrl = parameters.src.replace(/:\//, "://localhost/");	
				}
				parameters.src = tempUrl;
			}
			
			// Replace the default configuration with external configuration values.
			var fields:Dictionary = ConfigurationUtils.retrieveFields(PlayerConfiguration, false);
			for (var paramName:String in fields)
			{	
				var paramValue:String = parameters[paramName];
				if (parameters.hasOwnProperty(paramName))
				{										
					if 	(	paramValue != null
						&&  paramValue.length>0)
					{	
						var value:Object = playerConfiguration[paramName];
						// Use a param name convention for color properties.
						if (paramName.indexOf("Color")>0) 
						{
							paramValue = stripColorCode(paramValue);							
							var tmp:Number = parseInt("0x"+paramValue);
							// Ignore invalid values. keep the default.
							if (!isNaN(tmp) && tmp<=0xFFFFFF)
							{
								value = tmp;
							}
						}						
						else
						{
							switch (getDefinitionByName(fields[paramName]))
							{
								case Boolean:
									var lc:String =  paramValue.toLowerCase()
									if (lc == "true")
									{
										value = true;
									}
									else if (lc == "false")
									{
										value = false;
									} // else Leave the default value alone							
									break;	
								case int:
								case uint:
									var tempint:Number = parseInt(paramValue);
									if (!isNaN(tempint) && tempint>=0) // Only positive numbers are accepted
									{
										if (validators.hasOwnProperty(paramName))																
										{
											if (validators[paramName](paramName, tempint))
											{
												value = paramValue;
											}
										}	
										else
										{
											value = tempint;
										}
									}
									break;
								case Number:
									var tempnum:Number = parseFloat(paramValue);
									if (!isNaN(tempnum) && tempnum>=0) // Only positive numbers are accepted
									{
										value = tempnum;
									}
									break;	
								case String:
									if (validators.hasOwnProperty(paramName))																
									{
										if (validators[paramName](paramName, paramValue))
										{
											value = paramValue;
										}
									}	
									else
									{
										value = paramValue;
									}
									break;		
							}	
						}	
						playerConfiguration[paramName] = value;						
					}
				}
			} 	
			// Deserialize plugin configurations
			parameters["plugin_src"] = "http://osmf.org/metadata.swf";
			var resources:Vector.<MediaResourceBase> = new Vector.<MediaResourceBase>();
			deserializePluginConfigurations(parameters, resources);
			for each(var res:MediaResourceBase in resources)
			{
				if (res is URLResource && (res as URLResource).url == "http://osmf.org/metadata.swf")
				{
					for each(var ns:String in res.metadataNamespaceURLs)
					{
						playerConfiguration.assetMetadata[ns] = res.getMetadataValue(ns);
					}
				}
				else
				{
					playerConfiguration.pluginConfigurations.push(res);
				}
			}
		}
		
		/**
		 * Creates an array of PluginConfiguration objects out of a parameters array (flashvars).
		 * 
		 */ 
		public function deserializePluginConfigurations(parameters:Object, result:Vector.<MediaResourceBase>):void
		{
			var plugins:Object = new Object();
			var pluginNamespaces:Object = new Object();
			
			// First lets collect the names and src for all the plugins
			var paramName:String;
			var paramValue:String;
			var pluginName:String;
			
			var propertyName:String;
			var pos:int;
			var nssep:int;
			var nsalias:String;
			
			// The parameters array is being scanned twice because we need to create 
			// the list of valid plugins priors to deserializing their parameters.
			for (paramName in parameters)
			{	
				paramValue = parameters[paramName];
				if (paramName.indexOf(PLUGIN_PREFIX+PLUGIN_SEPARATOR)==0)
				{						
					pluginName = paramName.substring(PLUGIN_PREFIX.length+PLUGIN_SEPARATOR.length);
					// Ignore the plugins with names that match the PLUGIN_PREFIX
					// Ingore the plugins that contains the separator in their name
					if (pluginName!=PLUGIN_PREFIX && validateAgainstPatterns(pluginName, VALIDATION_PATTERN) )
					{
						var pluginConfiguration:MediaResourceBase;
						
						// if the src starts with http or file or ends with .swf, we assume it's an absolute/relative url to a dynamic plugin
						if ( (paramValue.substr(0, 4) == "http" 
							  	|| paramValue.substr(0, 4) == "file" 
								|| paramValue.substr(paramValue.length-4, 4) == ".swf") 
							 && validateURLProperty(paramName, paramValue, true))
						{
							pluginConfiguration = new URLResource(paramValue);
						}
						else if (ApplicationDomain.currentDomain.hasDefinition(paramValue))
						{
							// Assume this is a PluginInfo class
							var pluginInfoRef:Class = flash.utils.getDefinitionByName(paramValue) as Class;
							pluginConfiguration = new PluginInfoResource(new pluginInfoRef);
						}
						
						if(pluginConfiguration)
						{
							plugins[pluginName] = pluginConfiguration;
							pluginNamespaces[pluginName] = new Object();
							result.push(pluginConfiguration);
						}
					}
				}
			}
			
			// Now lets add the namespaces
			for (paramName in parameters)			
			{
				paramValue = parameters[paramName];
				pos = paramName.indexOf(PLUGIN_SEPARATOR); 
				if (pos>0)
				{
					pluginName = paramName.substring(0, pos);
					propertyName = paramName.substring(pos+1);
					if (pluginNamespaces.hasOwnProperty(pluginName))
					{
						if (propertyName.indexOf(NAMESPACE_PREFIX)==0)
						{
							nssep = propertyName.indexOf(NAMESPACE_SEPARATOR);
							nsalias = DEFAULT_NAMESPACE_NAME; 
							if (nssep>0)
							{
								nsalias = propertyName.substring(nssep+1);
							}
							if (nsalias!= NAMESPACE_PREFIX && validateAgainstPatterns(nsalias, VALIDATION_PATTERN))
							{
								pluginNamespaces[pluginName][nsalias] = new Object();
								pluginNamespaces[pluginName][nsalias][NAMESPACE_PREFIX] = paramValue;
							}
						}
					}
				}
			}
			
			// Now lets add additional parameters
			for (paramName in parameters)			
			{
				paramValue = parameters[paramName];
				pos = paramName.indexOf(PLUGIN_SEPARATOR); 
				if (pos>0)
				{
					pluginName = paramName.substring(0, pos);
					propertyName = paramName.substring(pos+1);
					if (plugins.hasOwnProperty(pluginName) && propertyName!=PLUGIN_PREFIX)
					{
						nssep = propertyName.indexOf(NAMESPACE_SEPARATOR);
						nsalias = DEFAULT_NAMESPACE_NAME; 
						if (nssep>0)
						{
							var temp:String = propertyName.substring(0, nssep);
							if (pluginNamespaces[pluginName].hasOwnProperty(temp))
							{
								nsalias = temp;
								propertyName = propertyName.substring(nssep+1);
							}
						}
						if (pluginNamespaces[pluginName].hasOwnProperty(nsalias))					
						{
							pluginNamespaces[pluginName][nsalias][propertyName] = paramValue;
						}
						else
						{
							if (!pluginNamespaces[pluginName].hasOwnProperty(ROOT))
							{
								pluginNamespaces[pluginName][ROOT] = new Object();
							}
							pluginNamespaces[pluginName][ROOT][propertyName] = paramValue;
						}
					}
				}
			}
			
			for (pluginName in pluginNamespaces)
			{
				for (nsalias in pluginNamespaces[pluginName])
				{					
					var namespace:String = pluginNamespaces[pluginName][nsalias][NAMESPACE_PREFIX];
					var pluginResource:MediaResourceBase = plugins[pluginName] as MediaResourceBase;
					if (nsalias == ROOT)
					{
						for (propertyName in pluginNamespaces[pluginName][nsalias])
						{
							pluginResource.addMetadataValue(propertyName, pluginNamespaces[pluginName][nsalias][propertyName]);
						}						
					}
					else
					{
						var pluginItem:Metadata = pluginResource.getMetadataValue(namespace) as Metadata;
						if (pluginItem==null)
						{	
							pluginItem = new StrobeDynamicMetadata();
						}
						for (propertyName in pluginNamespaces[pluginName][nsalias])
						{
							if (propertyName!=NAMESPACE_PREFIX)
							{
								pluginItem.addValue(propertyName, pluginNamespaces[pluginName][nsalias][propertyName]);
								pluginItem[propertyName] = pluginNamespaces[pluginName][nsalias][propertyName];
							}
						}
						pluginResource.addMetadataValue(namespace, pluginItem);
					}
				}
			}
		}
		
		// Internals		
		private function validateAgainstPatterns(value:String, pattern:RegExp):Boolean
		{
			var matches:Array = value.match(pattern);
			return (matches!=null) && (matches[0] == value); 
		}
		
		
		/**
		 * Validates an URL
		 */ 
		private function validateURLProperty(paramName:String, paramValue:String, isPluginUrl:Boolean = false):Boolean
		{
			// Validate the URL using the OSMF private api. We don't know if it's the best approach,
			// but we choose to do so because we want this validation to be consistent with the OSMF framework.			
			var url:URL = new URL(paramValue);
			// Checking the host name is enough for absolute paths.
			// For relative paths we only check that it's actually a swf file name if we are reffering to a plugin.
			if ( (url.absolute && url.host.length>0) 
				 || ( isPluginUrl 
				 		? paramValue.match(/^[^:]+swf$/) 
						: (url.path == url.rawUrl && paramValue.search("javascript") != 0)
					) )
			{
				return true;
			}
			return false;
		}

		/**
		 * Validates an URL
		 */ 
		private function validatePluginURLProperty(paramName:String, paramValue:String, isPluginUrl:Boolean = false):Boolean
		{
			// Validate the URL using the OSMF private api. We don't know if it's the best approach,
			// but we choose to do so because we want this validation to be consistent with the OSMF framework.			
			var url:URL = new URL(paramValue);
			// Checking the host name is enough for absolute paths.
			// For relative paths we only check that it's actually a swf file name if we are reffering to a plugin.
			if ( (url.absolute && url.host.length>0) 
				|| ( isPluginUrl 
					? paramValue.match(/^[^:]+swf$/) 
					: (url.path == url.rawUrl && paramValue.search("javascript") != 0)
				) )
			{
				return true;
			}
			return false;
		}
		
		/**
		 * Checks that an option is a valid value of an enumeration.
		 */ 
		private function validateEnumProperty(paramName:String, paramValue:Object):Boolean
		{
			var options:Array = enumerationValues[paramName];
			if (options.indexOf(paramValue)>=0)
			{
				return true;
			}	
			return false;
		}
		
		/**
		 * Removes the "#" and "0x" prefixes on strings representing colors
		 *  
		 * @param color
		 * @return string without the prepending "#" on "0x"
		 * 
		 */
		private function stripColorCode(color:String):String
		{
			var strippedColor:String = color;
			
			if (color.substring(0,1) == '#')
			{
				strippedColor = color.substring(1);
			}
			else if (color.substring(0,2) == '0x')
			{
				strippedColor = color.substring(2);
			}
			return strippedColor;
		}
		
		/**
		 * The list of accepted options for enumration properties.
		 * 
		 * This is used as a workarround for the lask of enumeration types in actionscript.
		 */ 
		private const enumerationValues:Object = 
		{
			scaleMode: [ScaleMode.LETTERBOX, ScaleMode.NONE, ScaleMode.STRETCH, ScaleMode.ZOOM],
			controlBarMode: ControlBarMode.values,
			videoRenderingMode:  VideoRenderingMode.values
		}
		
		/**
		 * Custom validators list
		 */ 
		private const validators:Object =
		{
			src: validateURLProperty,
			scaleMode: validateEnumProperty,
			controlBarMode: validateEnumProperty,
			videoRenderingMode: validateEnumProperty
		}
		
		/**
		 * The prefix which is used for identifying the plugins.
		 */ 
		private static const PLUGIN_PREFIX:String = "plugin";
		
		/**
		 * The prefix which is used for identifying the namespace parameter.
		 */ 
		private static const NAMESPACE_PREFIX:String = "namespace";
		
		/**
		 * The separator between the plugin prefix and their parameters.
		 */ 
		private static const PLUGIN_SEPARATOR:String = "_";
		private static const NAMESPACE_SEPARATOR:String = "_";
		private static const DEFAULT_NAMESPACE_NAME:String = "defaultNamespace";
		private static const ROOT:String = "roooooooooooot";
		
		private static const VALIDATION_PATTERN:RegExp = /[a-zA-Z][0-9a-zA-Z]*/;
	}
}