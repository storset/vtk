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

package org.osmf.player.media
{
	import flash.events.Event;
	import flash.events.TimerEvent;
	import flash.geom.Rectangle;
	import flash.utils.Timer;
	
	import org.osmf.elements.LightweightVideoElement;
	import org.osmf.events.*;
	import org.osmf.media.MediaElement;
	import org.osmf.media.MediaPlayer;
	import org.osmf.media.MediaPlayerState;
	import org.osmf.net.DynamicStreamingItem;
	import org.osmf.net.DynamicStreamingResource;
	import org.osmf.net.PlaybackOptimizationMetrics;
	import org.osmf.net.StreamType;
	import org.osmf.player.chrome.utils.MediaElementUtils;
	import org.osmf.player.configuration.VideoRenderingMode;
	import org.osmf.player.metadata.PlayerMetadata;
	import org.osmf.player.utils.StrobePlayerStrings;
	import org.osmf.player.utils.VideoRenderingUtils;
	import org.osmf.traits.DVRTrait;
	import org.osmf.traits.MediaTraitType;

	CONFIG::LOGGING
	{
		import org.osmf.logging.Log;
		import org.osmf.player.debug.StrobeLogger;
	}
	/**
	 * StrobeMediaPlayer is an optimized MediaPlayer. It is able to adjust it's settings for the best
	 * possible playback configuration based on the MediaElement properties. In a future version it will be able to ajust
	 * the playback configuration based on the computed Quality of Service metrics.
	 * 
	 * So far it is setting the optimal smoothing/deblocking settings for the current MediaElement.
	 * It is able to determine the optimal fullScreenSourceRect so that all the scaling is hardware accelerated.
	 * It is using a Double Threshold Buffer strategy.
	 * 
	 * Future versions will implement dynamic buffering strategies and other best practices related to flash video.
	 * 
	 * Important Note: StrobeMediaPlayer needs to be configured before setting the media. 
	 * The configuration settings changed after the media is set might be ignored. 
	 */ 
	public class StrobeMediaPlayer extends MediaPlayer
	{
		public var highQualityThreshold:uint = 480;
		public var videoRenderingMode:uint = VideoRenderingMode.AUTO;
		public var autoSwitchQuality:Boolean = true;
		
		/**
		 * Constructor.
		 * 
		 * @param media
		 */		
		public function StrobeMediaPlayer(media:MediaElement=null)
		{
			super(media);
			addEventListener(MediaPlayerCapabilityChangeEvent.IS_DYNAMIC_STREAM_CHANGE, onIsDynamicStreamChange);
			addEventListener(DisplayObjectEvent.MEDIA_SIZE_CHANGE, onMediaSizeChange);		
			addEventListener(DisplayObjectEvent.DISPLAY_OBJECT_CHANGE, onDisplayObjectChange);
						
			addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, onMediaPlayerStateChange);
			addEventListener(MediaElementChangeEvent.MEDIA_ELEMENT_CHANGE, onMediaElementChangeEvent);
		}
		
		public function get isDVRRecording():Boolean
		{
			return media.hasTrait(MediaTraitType.DVR) ? (media.getTrait(MediaTraitType.DVR) as DVRTrait).isRecording : false;
		}
		
		public function get isDVRLive():Boolean
		{
			return _isDVRLive;
		}

		public function set isDVRLive(value:Boolean):void
		{
			_isDVRLive = value;
		}

		public function get isLive():Boolean
		{
			if (streamType == StreamType.LIVE)
			{
				return true;
			}
			else if (streamType == StreamType.DVR)
			{
				return isDVRLive;
			}
			else
			{
				return false;
			}
		}
		
		public function get streamType():String
		{
			return _streamType;
		}

		public function set streamType(value:String):void
		{
			_streamType = value;
		}

		/**
		 * Retrieves the optimal fullScreenSourceRect so that all the scaling is hardware accelerated.
		 * 
		 * It considers both the video quality, video size and the size 
		 * of the monitor on which the video is being currently played.
		 */ 
		public function getFullScreenSourceRect(stageFullScreenWidth:int, stageFullScreenHeight:int):Rectangle
		{
			var rect:Rectangle = null;
			if (fullScreenVideoHeight > highQualityThreshold  && fullScreenVideoWidth > 0)
			{					
				rect
					= VideoRenderingUtils.computeOptimalFullScreenSourceRect
						( stageFullScreenWidth
						, stageFullScreenHeight
						, fullScreenVideoWidth
						, fullScreenVideoHeight
						);								
			}				
			return rect;
		}		
	
		public function seekUntilSuccess(position:Number, maxRepeatCount:uint = 10):void
		{
			var repeatCount:uint = 0;
			// WORKARROUND: FM-939 - HTTPStreamingDVR - the first seek always fails
			// http://bugs.adobe.com/jira/browse/FM-939
			var workarroundTimer:Timer = new Timer(2000, 1);
			workarroundTimer.addEventListener(TimerEvent.TIMER, 
				function (event:Event):void
				{					
					if (canSeek)
					{
						repeatCount ++;
						if (repeatCount < maxRepeatCount)
						{
							seek(position);
						}
					}
				}
			);
			
			addEventListener
				( SeekEvent.SEEKING_CHANGE
					, function(event:SeekEvent):void
					{									
						if (event.seeking == false)
						{
							removeEventListener(event.type, arguments.callee);						
							
							if (workarroundTimer != null)
							{
								// WORKARROUND: FM-939
								workarroundTimer.stop();
								workarroundTimer = null;
							}
						}
						else
						{	
							// WORKARROUND: FM-939
							if (workarroundTimer != null)
							{
								workarroundTimer.start();
							}
						}
					}
				);
			
			// Seek to the live position:
			seek(position);
		}
		
		public function snapToLive():Boolean
		{			
			if (isDVRRecording == false)
			{
				return false;
			}
			
			if (!playing)
			{
				play();
			}
			
			if (canSeek)
			{
				var livePosition:Number = Math.max(0, duration - bufferTime - 2); 
				if (canSeekTo(livePosition))
				{
					seekUntilSuccess(livePosition);
					isDVRLive = true;
					return true;
				}		
			}	
			return false;
		}
		
		
			
		// Handlers
		//		
		private function onMediaElementChangeEvent(event:MediaElementChangeEvent):void
		{
			if (media != null)
			{
				var mediaMetadata:PlayerMetadata;
				mediaMetadata = new PlayerMetadata();			
				mediaMetadata.mediaPlayer = this;
				media.metadata.addValue(PlayerMetadata.ID, mediaMetadata);
			}
		}
		
		private function onMediaPlayerStateChange(event:MediaPlayerStateChangeEvent):void
		{	
			// Computes Buffering QoS stats.
			if (event.state == MediaPlayerState.PLAYING)
			{
				if (isDynamicStream && autoDynamicStreamSwitch != autoSwitchQuality)
				{					
					autoDynamicStreamSwitch = autoSwitchQuality;
				}
			}			
		}
		
		private function onMediaSizeChange(event:DisplayObjectEvent):void
		{
			if (!isDynamicStream && event.newWidth > 0 && event.newHeight > 0)
			{
				fullScreenVideoWidth = event.newWidth;
				fullScreenVideoHeight = event.newHeight;
			}
			
			// Set the smothing and deblocking using best practices for HD/SD
			if (fullScreenVideoWidth > 0 && fullScreenVideoHeight > 0)
			{
				var lightweightVideoElement:LightweightVideoElement = MediaElementUtils.getMediaElementParentOfType(media, LightweightVideoElement) as LightweightVideoElement;
				if (lightweightVideoElement != null)
				{	
					if (isDynamicStream && fullScreenVideoHeight > event.newHeight)
					{					
						lightweightVideoElement.smoothing = true;
						lightweightVideoElement.deblocking = 0;
						CONFIG::LOGGING
						{	
						logger.info("Enabling smoothing/deblocking since the current resolution is lower then the best vertical resolution for this DynamicStream:" + fullScreenVideoHeight + "p");
						}
					}
					else
					{					
						lightweightVideoElement.smoothing 
							= VideoRenderingUtils.determineSmoothing
							(   videoRenderingMode
								, event.newHeight > highQualityThreshold 
							);
						lightweightVideoElement.deblocking 
							= VideoRenderingUtils.determineDeblocking
							(   videoRenderingMode
								, event.newHeight > highQualityThreshold
							);
						CONFIG::LOGGING
						{	
						logger.info("Updating smoothing & deblocking settings. smoothing=" + lightweightVideoElement.smoothing + " deblocking=" + lightweightVideoElement.deblocking);
						}
					}
				}
				else if (isDynamicStream && currentDynamicStreamIndex == maxAllowedDynamicStreamIndex)
				{
					// Update the fullScreenVideoWidth/Height since we didn't have them in the resource,
					// but we have it now since we are on the highest bitrate stream.					
					fullScreenVideoWidth = event.newWidth;
					fullScreenVideoHeight = event.newHeight;
				}
			}
		}
		
		private function onDisplayObjectChange(event:Event):void
		{
			var newStreamType:String = MediaElementUtils.getStreamType(media);
			if (newStreamType != streamType)
			{
				streamType = newStreamType;
				CONFIG::LOGGING
				{
					logger.qos.streamType = streamType;
				}
				var mediaMetadata:PlayerMetadata;
				mediaMetadata = new PlayerMetadata();			
				mediaMetadata.mediaPlayer = this;
				media.metadata.addValue(PlayerMetadata.ID, mediaMetadata);
			}
			
		}
		
		private function onIsDynamicStreamChange(event:Event):void
		{	
			if (isDynamicStream)
			{
				// Apply the configuration's autoSwitchQuality setting:
				this.autoDynamicStreamSwitch = false;
				// Retrieve the highest quality stream item 
				var dynamicStreamingResource:DynamicStreamingResource = MediaElementUtils.getResourceFromParentOfType(media, DynamicStreamingResource) as DynamicStreamingResource;
				var dynamicStreamingItem:DynamicStreamingItem;
				if (dynamicStreamingResource != null)
				{
					// Add cast, as a workarround for some strange compile errors in my Flash Builder
					dynamicStreamingItem = DynamicStreamingItem(dynamicStreamingResource.streamItems[dynamicStreamingResource.streamItems.length-1]);
					
					// Pass the width/height to the fullScreenController so that it is able to optimize the size of the fullScreenSourceRect.
					fullScreenVideoWidth = dynamicStreamingItem.width;
					fullScreenVideoHeight = dynamicStreamingItem.height;
					CONFIG::LOGGING
					{
					logger.qos.ds.bestHorizontatalResolution = fullScreenVideoWidth;
					logger.qos.ds.bestVerticalResolution = fullScreenVideoHeight;
					}
				}
			}
		}
		private var _isDVRLive:Boolean;
		private var _streamType:String;
		private var fullScreenVideoWidth:uint = 0;
		private var fullScreenVideoHeight:uint = 0;
		
		CONFIG::LOGGING
		{
		protected var logger:StrobeLogger = Log.getLogger("StrobeMediaPlayback") as StrobeLogger;
		}
	}
}