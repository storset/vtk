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
	import org.osmf.layout.ScaleMode;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.URLResource;
	import org.osmf.metadata.Metadata;
	import org.osmf.net.StreamType;

	/**
	 * Player configuration data model
	 */ 		
	public class PlayerConfiguration
	{
		/** The location of the mediafile. */
		public var src:String = "";
		
		/** Contains the asset metadata */
		public var assetMetadata:Object = new Object();
		
		/** The background color of the player */ 
		public var backgroundColor:uint = 0;
		
		/** Tint color */ 
		public var tintColor:uint = 0;
		
		/** Tels wether the player should auto hide controls */ 
		public var controlBarAutoHide:Boolean = true;
		
		/** The location of the control bar */ 
		public var controlBarMode:String = ControlBarMode.DOCKED;		
		
		/** Tels whether the media should be played in a loop */ 
		public var loop:Boolean = false;
		
		/** Tels whether the media should autostart */ 
		public var autoPlay:Boolean = false;
		
		/**
		 * Scale mode as defined here:
		 * http://help.adobe.com/en_US/FlashPlatform/beta/reference/actionscript/3/org/osmf/display/ScaleMode.html
		 */ 
		public var scaleMode:String = ScaleMode.LETTERBOX;
		
		/** Defines the file that holds the player's skin */
		public var skin:String = "";
		
		/** Defines if messages will show verbose or not */ 
		public var verbose:Boolean = false;
		
		/** Defines the path to the image to show before the main content shows */
		public var poster:String = "";
	
		/** Defines if the play button overlay appears */
		public var playButtonOverlay:Boolean = true;
		
		/** Defines if the buffering overlay appears */
		public var bufferingOverlay:Boolean = true;
		
		/** Defines the high quality threshold */
		public var highQualityThreshold:uint = 480;
		
		/** Defines the video rendering mode */
		public var videoRenderingMode:uint = VideoRenderingMode.AUTO;
		
		/** Defines the auto switch quality */
		public var autoSwitchQuality:Boolean = true;
		
		/** Defines the optimizeInitialIndex flag */ 
		public var optimizeInitialIndex:Boolean = true
			
		/** Defines the optimized buffering flag */
		public var optimizeBuffering:Boolean = true;
		
		/** Defines the stream type */
		public var streamType:String = StreamType.LIVE_OR_RECORDED;		
		
		/** Indicates, for RTMP streaming URLs, whether the URL includes the FMS application instance or not. */
		public var urlIncludesFMSApplicationInstance : Boolean = false;
			
		/** Defines the initial buffer time for video content */
		public var initialBufferTime:Number = 0.1;		
		
		/** Defines the expanded buffer time for video content */
		public var expandedBufferTime:Number = 10;	
		
		/** Defines the buffer time for dynamic streams */
		public var dynamicStreamBufferTime:Number = 0;
		
		/** Defines the minimal continuous playback time */
		public var minContinuousPlaybackTime:Number = 30;
		
		/** Defines the collection of plug-in configurations */
		public var pluginConfigurations:Vector.<MediaResourceBase> = new Vector.<MediaResourceBase>();
	}
}