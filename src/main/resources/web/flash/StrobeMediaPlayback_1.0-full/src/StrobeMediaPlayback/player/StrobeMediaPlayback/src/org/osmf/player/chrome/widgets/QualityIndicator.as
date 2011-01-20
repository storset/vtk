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

package org.osmf.player.chrome.widgets
{
	import __AS3__.vec.Vector;
	
	import flash.display.Bitmap;
	import flash.display.DisplayObject;
	import flash.events.Event;
	
	import org.osmf.player.chrome.assets.AssetIDs;
	import org.osmf.player.chrome.assets.AssetsManager;
	import org.osmf.events.DynamicStreamEvent;
	import org.osmf.media.MediaElement;
	import org.osmf.traits.DynamicStreamTrait;
	import org.osmf.traits.MediaTraitType;
	
	
	public class QualityIndicator extends Widget
	{
		// Public Interface
		//
		
		public var hdOnFace:String = AssetIDs.HD_ON;
		public var hdOffFace:String = AssetIDs.HD_OFF;
		
		// Overrides
		//
		
		override public function configure(xml:XML, assetManager:AssetsManager):void
		{
			hdOn = assetManager.getDisplayObject(hdOnFace);
			hdOff = assetManager.getDisplayObject(hdOffFace);
			
			face = hdOffFace;
			
			super.configure(xml, assetManager);
		}
		
		override protected function get requiredTraits():Vector.<String>
		{
			return _requiredTraits;
		}
		
		override protected function processMediaElementChange(oldElement:MediaElement):void
		{
			visibilityDeterminingEventHandler();
		}
		
		override protected function processRequiredTraitsAvailable(element:MediaElement):void
		{
			dynamicStream = element.getTrait(MediaTraitType.DYNAMIC_STREAM) as DynamicStreamTrait;
			dynamicStream.addEventListener(DynamicStreamEvent.SWITCHING_CHANGE, visibilityDeterminingEventHandler);
			dynamicStream.addEventListener(DynamicStreamEvent.NUM_DYNAMIC_STREAMS_CHANGE, visibilityDeterminingEventHandler);
			
			visibilityDeterminingEventHandler();
		}
		
		override protected function processRequiredTraitsUnavailable(element:MediaElement):void
		{
			if (dynamicStream)
			{
				dynamicStream.removeEventListener(DynamicStreamEvent.SWITCHING_CHANGE, visibilityDeterminingEventHandler);
				dynamicStream.removeEventListener(DynamicStreamEvent.NUM_DYNAMIC_STREAMS_CHANGE, visibilityDeterminingEventHandler);
				dynamicStream = null;
			}
			
			visibilityDeterminingEventHandler();
		}
		
		// Internals
		//
		
		private function visibilityDeterminingEventHandler(event:Event = null):void
		{
			visible = media != null && dynamicStream != null;
			if (visible)
			{
				var face:DisplayObject
					= (dynamicStream.currentIndex + 1) > (dynamicStream.numDynamicStreams/2)
						? hdOn
						: hdOff;
				
				if (numChildren > 0 && getChildAt(0) != face)
				{
					removeChildAt(0);
				}
				
				if (face && contains(face) == false)
				{
					addChildAt(face, 0);
				}
					
				width = face ? face.width + (layoutMetadata.paddingRight || 0) : 0;
				height = face ? face.height : 0;
				
				measure();
			}
		}
		
		private var dynamicStream:DynamicStreamTrait;
		private var hdOn:DisplayObject;
		private var hdOff:DisplayObject;
		
		/* static */
		private static const _requiredTraits:Vector.<String> = new Vector.<String>;
		_requiredTraits[0] = MediaTraitType.DYNAMIC_STREAM;
	}
}