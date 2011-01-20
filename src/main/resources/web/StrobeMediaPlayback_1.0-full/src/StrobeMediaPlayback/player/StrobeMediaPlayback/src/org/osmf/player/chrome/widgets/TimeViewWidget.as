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
	import flash.events.Event;
	import flash.events.TimerEvent;
	import flash.geom.PerspectiveProjection;
	import flash.media.Microphone;
	import flash.text.TextFormat;
	import flash.text.TextFormatAlign;
	import flash.utils.Timer;
	
	import org.osmf.events.MediaElementEvent;
	import org.osmf.events.MetadataEvent;
	import org.osmf.events.SeekEvent;
	import org.osmf.events.TimeEvent;
	import org.osmf.layout.HorizontalAlign;
	import org.osmf.layout.LayoutMode;
	import org.osmf.layout.VerticalAlign;
	import org.osmf.media.MediaElement;
	import org.osmf.metadata.Metadata;
	import org.osmf.net.StreamType;
	import org.osmf.player.chrome.assets.AssetsManager;
	import org.osmf.player.chrome.metadata.ChromeMetadata;
	import org.osmf.player.chrome.utils.FormatUtils;
	import org.osmf.player.media.StrobeMediaPlayer;
	import org.osmf.player.metadata.PlayerMetadata;
	import org.osmf.traits.MediaTraitType;
	import org.osmf.traits.SeekTrait;
	import org.osmf.traits.TimeTrait;

	/**
	 * TimeViewWidget displays the current time and the total duration of the media.
	 * 
	 */ 
	public class TimeViewWidget extends Widget
	{
		/**
		 * Returns the current textual represention of the time displayed by the TimeViewWidget.
		 */ 
		internal function get text():String
		{
			return 	currentTimeLabel.text 
				+ (timeSeparatorLabel.visible ? timeSeparatorLabel.text : "") 
				+ (totalTimeLabel.visible ? totalTimeLabel.text : "");
		}
		
		/**
		 * Updates the displayed text based on the existing traits.
		 */ 
		internal function updateNow():void
		{
			var timeTrait:TimeTrait;
			timeTrait = media.getTrait(MediaTraitType.TIME) as TimeTrait;			
			updateValues(timeTrait.currentTime, timeTrait.duration, live);
		}
		
		/**
		 * Updates the displayed text using the time values provided as arguments.
		 */ 
		internal function updateValues(currentTimePosition:Number, totalDuration:Number, isLive:Boolean):void
		{	
			// Don't display the time labels if total duration is 0
			if (isNaN(totalDuration) || totalDuration == 0) 
			{	
				if (isLive)
				{
					// WORKARROUND: adding additional spaces since I'm not able to position the text nicely
					currentTimeLabel.text = LIVE + "   ";					
					currentTimeLabel.autoSize = false;
					currentTimeLabel.width = currentTimeLabel.measuredWidth;
					currentTimeLabel.align = TextFormatAlign.RIGHT;
				}
				if (currentTimePosition > 0 || isLive)
				{
					totalTimeLabel.visible = false;
					timeSeparatorLabel.visible = false;
				}
			}
			else
			{
				totalTimeLabel.visible = true;
				timeSeparatorLabel.visible = true;
				var newValues:Vector.<String> = FormatUtils.formatTimeStatus(currentTimePosition, totalDuration, isLive, LIVE);
				
				// WORKARROUND: adding additional spaces since I'm unable to position the text nicely
				var currentTimeString:String = " " + newValues[0] + " ";
				var totalTimeString:String = " " + newValues[1] + " ";
				
				totalTimeLabel.text = totalTimeString;			
				if (currentTimeLabel.autoSize)
				{
					currentTimeLabel.text = totalTimeString;
					currentTimeLabel.autoSize = false;
					currentTimeLabel.width = currentTimeLabel.measuredWidth;
					currentTimeLabel.align = TextFormatAlign.RIGHT;
				}
				currentTimeLabel.text = currentTimeString;
			}
		}
		
		// Overrides
		//
		
		override public function configure(xml:XML, assetManager:AssetsManager):void
		{		
			setSuperVisible(false);
			layoutMetadata.percentHeight = 100;
			layoutMetadata.layoutMode = LayoutMode.HORIZONTAL;
			layoutMetadata.horizontalAlign = HorizontalAlign.RIGHT;
			layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
			
			// Current time
			currentTimeLabel = new LabelWidget();			
			currentTimeLabel.autoSize = true;
			currentTimeLabel.layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
			currentTimeLabel.layoutMetadata.horizontalAlign = HorizontalAlign.RIGHT;			
			addChildWidget(currentTimeLabel);
			
			// Separator
			timeSeparatorLabel = new LabelWidget();			
			timeSeparatorLabel.autoSize = true;
			timeSeparatorLabel.layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
			timeSeparatorLabel.layoutMetadata.horizontalAlign = HorizontalAlign.RIGHT;
			addChildWidget(timeSeparatorLabel);
			
			// Duration
			totalTimeLabel = new LabelWidget();
			totalTimeLabel.autoSize = true;
			totalTimeLabel.layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
			totalTimeLabel.layoutMetadata.horizontalAlign = HorizontalAlign.RIGHT;
			addChildWidget(totalTimeLabel);
			
			currentTimeLabel.configure(xml, assetManager);
			totalTimeLabel.configure(xml, assetManager);
			timeSeparatorLabel.configure(xml, assetManager);
			
			super.configure(xml, assetManager);	
			
			currentTimeLabel.text = TIME_ZERO;
			timeSeparatorLabel.text = "/";
			totalTimeLabel.text = TIME_ZERO;
			measure();
		}
		
		override protected function processRequiredTraitsAvailable(element:MediaElement):void
		{
			timer.addEventListener(TimerEvent.TIMER, onTimerEvent);
			timer.start();
			
			setSuperVisible(true);
		}
		
		override protected function processRequiredTraitsUnavailable(element:MediaElement):void
		{		
			timer.stop();
			setSuperVisible(false);
		}
		
		override protected function get requiredTraits():Vector.<String>
		{
			return _requiredTraits;
		}
			
		override protected function onMediaElementTraitAdd(event:MediaElementEvent):void
		{
			currentTimeLabel.autoSize = true;
			if (event.traitType == MediaTraitType.SEEK)
			{
				seekTrait = media.getTrait(MediaTraitType.SEEK) as SeekTrait;
				seekTrait.addEventListener(SeekEvent.SEEKING_CHANGE, onSeekingChange);
			}
		}
		
		override protected function onMediaElementTraitRemove(event:MediaElementEvent):void
		{	
			if (event.traitType == MediaTraitType.SEEK && seekTrait != null)
			{
				seekTrait.removeEventListener(SeekEvent.SEEKING_CHANGE, onSeekingChange);
				seekTrait = null;				
			}
		}

		// Internals
		//
		private static const _requiredTraits:Vector.<String> = new Vector.<String>;
		_requiredTraits[0] = MediaTraitType.TIME;
		private static const LIVE:String = "Live";
		private static const TIME_ZERO:String = " 0:00 ";
		
		private var currentTimeLabel:LabelWidget;
		private var timeSeparatorLabel:LabelWidget;
		private var totalTimeLabel:LabelWidget;	
		
		private var seekTrait:SeekTrait;
		private var timer:Timer = new Timer(1000);
		private var maxLength:uint = 0;
		private var maxWidth:Number = 100;
		
		private function get live():Boolean
		{	
			var mediaMetadata:PlayerMetadata;
			mediaMetadata = media.metadata.getValue(PlayerMetadata.ID) as PlayerMetadata;
			if (mediaMetadata != null)
			{
				var mediaPlayer:StrobeMediaPlayer;
				mediaPlayer = mediaMetadata.mediaPlayer;
				return mediaPlayer.isLive;
			}
			return false;
		}
		
		private function onTimerEvent(event:Event):void
		{
			updateNow();
		}
		
		private function onSeekingChange(event:SeekEvent):void
		{
			var timeTrait:TimeTrait;
			timeTrait = media.getTrait(MediaTraitType.TIME) as TimeTrait;			
			
			if (event.seeking)
			{
				updateValues(event.time, timeTrait.duration, live);
				timer.stop();				
			}
			else
			{
				updateValues(event.time, timeTrait.duration, live);
				timer.start();
			}
		}	
	}
}