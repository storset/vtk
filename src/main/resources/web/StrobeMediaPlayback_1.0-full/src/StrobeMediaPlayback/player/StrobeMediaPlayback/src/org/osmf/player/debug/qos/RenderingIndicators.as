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

package org.osmf.player.debug.qos
{
	
	/**
	 * Stores the qos indicators related to Rendering. 
	 */ 
	public class RenderingIndicators extends IndicatorsBase
	{
		public var width:uint;
		public var height:uint;
		public var aspectRatio:Number;	
		public var HD:Boolean;
		public var smoothing:Boolean;
		public var deblocking:String;
		public var fullScreenSourceRect:String;
		public var fullScreenSourceRectAspectRatio:Number;
		public var screenWidth:Number;
		public var screenHeight:Number;
		public var screenAspectRatio:Number;
		
		override protected function getOrderedFieldList():Array
		{
			return [
				"width",
				"height",
				"aspectRatio",
				"HD",
				"smoothing",
				"deblocking", 
				"fullScreenSourceRect",
				"fullScreenSourceRectAspectRatio",
				"screenWidth",
				"screenHeight",
				"screenAspectRatio",
			];
		}
	}
}