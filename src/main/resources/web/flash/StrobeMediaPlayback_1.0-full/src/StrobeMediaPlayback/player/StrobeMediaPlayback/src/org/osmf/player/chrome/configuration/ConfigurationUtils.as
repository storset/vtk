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

package org.osmf.player.chrome.configuration
{
	import flash.utils.Dictionary;
	import flash.utils.describeType;
	import flash.utils.getDefinitionByName;
	import flash.utils.getQualifiedClassName;

	public class ConfigurationUtils
	{

		/**
		 * Return a dictionary describing every settable field on this object or class.
		 *
		 * Fields are indexed by name, and the type is contained as a string.
		 */
		public static function retrieveFields(c:*, ignoreReadOnly:Boolean = true):Dictionary
		{
			if (!(c is Class))
			{
				// Convert to its class.
				c = getDefinitionByName(getQualifiedClassName(c));
			}
			
			// Otherwise describe the type...
			var typeXml:XML = describeType(c);
			
			// Set up the dictionary
			var typeDict:Dictionary = new Dictionary();
			
			// Walk all the variables...
			for each (var variable:XML in typeXml.factory.variable)
			typeDict[variable.@name.toString()] = variable.@type.toString();
			
			// And all the accessors...
			for each (var accessor:XML in typeXml.factory.accessor)
			{
				// Ignore ones we can't write to.
				if (ignoreReadOnly && accessor.@access == "readonly")
					continue;
				
				typeDict[accessor.@name.toString()] = accessor.@type.toString();
			}
			
			return typeDict;
		}
	}
}