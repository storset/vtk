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

package org.osmf.player.chrome.assets
{
	import flash.display.Bitmap;
	import flash.display.BitmapData;
	import flash.display.DisplayObject;
	import flash.display.InteractiveObject;

	public class SymbolAsset extends DisplayObjectAsset
	{
		public function SymbolAsset(type:Class)
		{
			_type = type;
			super();
		}
		
		public function get type():Class
		{
			return _type;
		}
		
		override public function get displayObject():DisplayObject
		{
			var instance:* = new type();
			if (instance is BitmapData)
			{
				instance = new Bitmap(instance);
			}
			return instance as DisplayObject;
		}
		private var _type:Class;
	}
}