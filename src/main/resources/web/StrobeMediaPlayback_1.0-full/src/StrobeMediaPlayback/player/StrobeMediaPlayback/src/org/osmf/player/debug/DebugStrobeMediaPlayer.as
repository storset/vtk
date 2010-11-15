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

package org.osmf.player.debug
{
	import org.osmf.player.chrome.utils.MediaElementUtils;
	import org.osmf.elements.LightweightVideoElement;
	import org.osmf.events.DisplayObjectEvent;
	import org.osmf.events.DynamicStreamEvent;
	import org.osmf.events.MediaErrorEvent;
	import org.osmf.events.MediaPlayerCapabilityChangeEvent;
	import org.osmf.events.MediaPlayerStateChangeEvent;
	import org.osmf.media.MediaPlayerState;
	import org.osmf.net.DynamicStreamingResource;
	import org.osmf.player.media.StrobeMediaPlayer;
	import org.osmf.traits.DynamicStreamTrait;
	import org.osmf.traits.MediaTraitType;

	/**
	 * An extension to the StrobeMediaPlayer class which is responsible 
	 * for tracking qos information.
	 */  
	public class DebugStrobeMediaPlayer extends StrobeMediaPlayer
	{
		/**
		 * Constructor. Logs all the MediaPlayer events.
		 * Additionally it registers handlers for the main
		 * player state changes so that it updates the QoS stats.
		 */ 
		public function DebugStrobeMediaPlayer()
		{		
			addEventListener(MediaPlayerCapabilityChangeEvent.IS_DYNAMIC_STREAM_CHANGE, logger.event);
			addEventListener(DisplayObjectEvent.MEDIA_SIZE_CHANGE, logger.event);
			addEventListener(DynamicStreamEvent.SWITCHING_CHANGE, logger.event);
			addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, logger.event);
			addEventListener(MediaErrorEvent.MEDIA_ERROR, logger.event);
			addEventListener(MediaPlayerCapabilityChangeEvent.TEMPORAL_CHANGE, logger.event);
		
			addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, onMediaPlayerStateChange);
			
			addEventListener(DynamicStreamEvent.SWITCHING_CHANGE, onSwitchingChange);
			addEventListener(DisplayObjectEvent.MEDIA_SIZE_CHANGE, onMediaSizeChange);
		}
		
		// Internals
		//		
	
		private function onMediaPlayerStateChange(event:MediaPlayerStateChangeEvent):void
		{	
			// Computes Buffering QoS stats.
			if (event.state == MediaPlayerState.PLAYING)
			{
				logger.qos.buffer.eventCount++;
				var now:Number = new Date().time;			
				logger.qos.buffer.previousWaitDuration = now - bufferingStartTimestamp;
				logger.qos.buffer.totalWaitDuration += logger.qos.buffer.previousWaitDuration;
				logger.qos.buffer.avgWaitDuration = logger.qos.buffer.totalWaitDuration / logger.qos.buffer.eventCount;
				logger.qos.buffer.maxWaitDuration = Math.max(logger.qos.buffer.maxWaitDuration, logger.qos.buffer.previousWaitDuration);
				
				bufferingStartTimestamp = NaN;
			}
			if (event.state == MediaPlayerState.BUFFERING)
			{	
				bufferingStartTimestamp = new Date().time;	
			}			
		}
		
		/**
		 * Updates the Rendering and DynamicStreaming QoS indicators. 
		 */ 
		private function onMediaSizeChange(event:DisplayObjectEvent):void
		{		
			width = event.newWidth;
			height = event.newHeight;
			logger.qos.rendering.width = event.newWidth;
			logger.qos.rendering.height = event.newHeight;
			logger.qos.rendering.aspectRatio = width / height;
			logger.qos.ds.currentVerticalResolution = height;
			var lightweightVideoElement:LightweightVideoElement = MediaElementUtils.getMediaElementParentOfType(media, LightweightVideoElement) as LightweightVideoElement;
			if (lightweightVideoElement != null)
			{
				logger.qos.rendering.HD = event.newHeight > highQualityThreshold;
				logger.qos.rendering.smoothing = lightweightVideoElement.smoothing;
				logger.qos.rendering.deblocking = lightweightVideoElement.deblocking == 0 ? "Lets the video compressor apply the deblocking filter as needed." : "Does not use a deblocking filter";
				
				if (isDynamicStream)
				{
					logger.qos.ds.index = currentDynamicStreamIndex;
					logger.qos.ds.numDynamicStreams = numDynamicStreams;
					logger.qos.ds.currentBitrate = getBitrateForDynamicStreamIndex(currentDynamicStreamIndex);
				}
			}			
		}
		
		/**
		 * Updates the Dynamic Streaming QoS Indicators.
		 */ 
		private function onSwitchingChange(dynamicStreamEvent:DynamicStreamEvent):void
		{
			var dsTrait:DynamicStreamTrait;
			dsTrait = media.getTrait(MediaTraitType.DYNAMIC_STREAM) as DynamicStreamTrait;
			var now:Date = new Date();
			var switchingDuration:int;
			
			var index:int = dsTrait.currentIndex;
			var bitrate:Number = dsTrait.getBitrateForIndex(index);
			
			logger.qos.ds.index = index;
			logger.qos.ds.currentBitrate = bitrate; 
			if (dynamicStreamEvent.switching)
			{
				swichingStartTime = now;
				previousIndex = dsTrait.currentIndex;
				previousBitrate = dsTrait.getBitrateForIndex(dsTrait.currentIndex);				
			}
			else
			{						
				logger.qos.ds.previousSwitchDuration = (new Date()).time - swichingStartTime.time;
				logger.qos.ds.totalSwitchDuration += logger.qos.ds.previousSwitchDuration;
				logger.qos.ds.dsSwitchEventCount ++;
				logger.qos.ds.avgSwitchDuration = logger.qos.ds.totalSwitchDuration / logger.qos.ds.dsSwitchEventCount;
				logger.info("Switch complete. Previous (index, bitrate)=({0},{1}). Current (index, bitrate)=({2},{3})", previousIndex, previousBitrate, index, bitrate);
			}	
		}
		
		private var bufferingStartTimestamp:Number;
		
		private var swichingStartTime:Date;
		private var width:Number;
		private var height:Number;	
		private var previousIndex:uint;
		private var previousBitrate:Number;	
	}
}