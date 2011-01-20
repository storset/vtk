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
	import org.osmf.media.DefaultMediaFactory;
	import org.osmf.media.MediaElement;
	import org.osmf.media.MediaFactoryItem;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.net.HTTPStreamingNetLoaderAdapter;
	import org.osmf.net.NetLoader;
	import org.osmf.net.StreamingURLResource;
	CONFIG::FLASH_10_1
	{
	import org.osmf.net.httpstreaming.HTTPStreamingNetLoader;
	}
	import org.osmf.net.rtmpstreaming.RTMPDynamicStreamingNetLoader;
	import org.osmf.player.configuration.PlayerConfiguration;
	import org.osmf.player.elements.PlaylistElement;
	import org.osmf.player.elements.playlistClasses.PlaylistLoader;
	import org.osmf.elements.VideoElement;
	import org.osmf.net.RTMPDynamicStreamingNetLoaderAdapter;
	import org.osmf.net.PlaybackOptimizationManager;
	import org.osmf.player.elements.ErrorElement;
	import org.osmf.player.chrome.assets.AssetsManager;
	import org.osmf.player.chrome.ChromeProvider;
	import org.osmf.elements.DurationElement;
	import org.osmf.events.MediaError;
	import org.osmf.player.errors.ErrorTranslator;

	/**
	 * StrobeMediaFactory is extending the DefaultMediaFactory by adding a playlist(.m3u) loader
	 * and optimized buffer management loaders. 
	 * Note that the buffer management loaders will probably be integrated into OSMF 1.5. 
	 */ 
	public class StrobeMediaFactory extends DefaultMediaFactory
	{
		public function StrobeMediaFactory(configuration:PlayerConfiguration)
		{	
			super();
			
			this.configuration = configuration;
			assetManager = ChromeProvider.getInstance().assetManager;
			
			addItem
				( new MediaFactoryItem
					( "org.osmf.player.elements.PlaylistElement"
					, new PlaylistLoader().canHandleResource
					, playlistElementConstructor
					)
				);
				
			if (configuration.optimizeBuffering || configuration.optimizeInitialIndex)
			{
				var playbackOptimizationManager:PlaybackOptimizationManager
					= createPlaybackOptimizationManager(configuration);
			
				CONFIG::FLASH_10_1
				{
				addStrobeAdapterToItemById("org.osmf.elements.video.httpstreaming", createHTTPStreamingNetLoaderAdapter(playbackOptimizationManager));
				}
				addStrobeAdapterToItemById("org.osmf.elements.video.rtmpdynamicStreaming", createRTMPDynamicStreamingNetLoaderAdapter(playbackOptimizationManager));
			}
		}
		
		// Protected
		//
		protected function createPlaybackOptimizationManager(configuration:PlayerConfiguration):PlaybackOptimizationManager
		{
			return new PlaybackOptimizationManager(configuration);
		}
		
		CONFIG::FLASH_10_1
		{
		protected function createHTTPStreamingNetLoaderAdapter(playbackOptimizationManager:PlaybackOptimizationManager):NetLoader
		{
			return new HTTPStreamingNetLoaderAdapter(playbackOptimizationManager);
		}
		}
		
		protected function createRTMPDynamicStreamingNetLoaderAdapter(playbackOptimizationManager:PlaybackOptimizationManager):NetLoader
		{
			return new RTMPDynamicStreamingNetLoaderAdapter(playbackOptimizationManager);
		}
		
		// Internals
		//
		
		private var configuration:PlayerConfiguration;
		private var assetManager:AssetsManager;
		
		private function addStrobeAdapterToItemById(id:String, netLoaderStrobeAdapter:NetLoader):void
		{	
			addItem
				( new MediaFactoryItem
					( id
					, netLoaderStrobeAdapter.canHandleResource
					, function():MediaElement
						{
							var videoElement:VideoElement = new VideoElement(null, netLoaderStrobeAdapter);
							VideoElementRegistry.getInstance().register(videoElement);
							return videoElement;
						}
					)
				);
		}
		
		private function playlistElementConstructor():MediaElement
		{
			return new PlaylistElement
				( new PlaylistLoader
					( this
					, playlistLoaderResourceConstructor
					, playlistLoaderErrorElementConstructor
					)
				);
		}
		
		private function playlistLoaderResourceConstructor(url:String):MediaResourceBase
		{
			return new StreamingURLResource(url, configuration.streamType);
		}
		
		private function playlistLoaderErrorElementConstructor(error:Error):MediaElement
		{
			var message:String;
			if (configuration != null && configuration.verbose == false)
			{
				message = ErrorTranslator.translate(error).message;
			}
			else
			{
				message = error.message;
				if (error.hasOwnProperty("detail"))
				{
					message += "\n" + error["detail"];
				}
			}
			
			return new DurationElement(5, new ErrorElement("Playlist element failed playback:\n" + message));
		}
	}
}