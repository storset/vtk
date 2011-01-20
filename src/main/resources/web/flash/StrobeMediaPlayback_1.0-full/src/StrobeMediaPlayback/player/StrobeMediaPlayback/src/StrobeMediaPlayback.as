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

package
{	
	import flash.display.*;
	import flash.events.*;
	import flash.external.ExternalInterface;
	import flash.system.Capabilities;
	import flash.ui.Mouse;
	import flash.utils.Timer;
	
	import org.osmf.containers.MediaContainer;
	import org.osmf.elements.*;
	import org.osmf.events.*;
	import org.osmf.layout.*;
	import org.osmf.media.*;
	import org.osmf.metadata.Metadata;
	import org.osmf.net.StreamingURLResource;
	import org.osmf.player.chrome.ChromeProvider;
	import org.osmf.player.chrome.assets.AssetsManager;
	import org.osmf.player.chrome.events.WidgetEvent;
	import org.osmf.player.chrome.widgets.BufferingOverlay;
	import org.osmf.player.chrome.widgets.PlayButtonOverlay;
	import org.osmf.player.configuration.*;
	import org.osmf.player.elements.*;
	import org.osmf.player.elements.playlistClasses.*;
	import org.osmf.player.errors.*;
	import org.osmf.player.media.*;
	import org.osmf.player.plugins.PluginLoader;
	import org.osmf.player.utils.StrobePlayerStrings;
	import org.osmf.traits.LoadTrait;
	import org.osmf.traits.MediaTraitType;
	import org.osmf.utils.OSMFStrings;
	
	CONFIG::LOGGING
	{
		import org.osmf.player.debug.DebugStrobeMediaPlayer;
		import org.osmf.player.debug.LogHandler;
		import org.osmf.player.debug.StrobeLoggerFactory;
		import org.osmf.player.debug.StrobeLogger;
		import org.osmf.logging.Log;
		import org.osmf.elements.LightweightVideoElement;
	}
	/**
	 * StrobeMediaPlayback is responsible for initializing a StrobeMediaPlayer and
	 * setting up the control bar behaviour and layout.
	 */
	[SWF(frameRate="25")]
	public class StrobeMediaPlayback extends Sprite
	{
		// These should be accessible from the preloader for the performance measurement to work.
		public var player:StrobeMediaPlayer;		
		public var factory:MediaFactory;		
		
		public function StrobeMediaPlayback()
		{			
			super();
			
			addEventListener(Event.ADDED_TO_STAGE, onAddedToStage);
			
			CONFIG::LOGGING
			{
				Log.loggerFactory = new StrobeLoggerFactory(new LogHandler());
				logger = Log.getLogger("StrobeMediaPlayback") as StrobeLogger;
			}
		}
		
		/**
		 * Initializes the player with the parameters and it's context (stage).
		 * 
		 * We need the stage at this point because we need 
		 * to setup the fullscreen event handlers in the initialization phase.
		 */ 
		public function initialize(parameters:Object, stage:Stage):void
		{
			removeEventListener(Event.ADDED_TO_STAGE, onAddedToStage);
			
			CONFIG::FLASH_10_1
			{
				// Register the global error handler.
				if (loaderInfo != null && loaderInfo.hasOwnProperty("uncaughtErrorEvents"))
				{
					loaderInfo["uncaughtErrorEvents"].addEventListener(UncaughtErrorEvent.UNCAUGHT_ERROR, onUncaughtError);
				}
			}
			
			// Keep a reference to the stage (when a preloader is used, the
			// local stage property is null at this time):
			_stage = stage;
			
			var assetManager:AssetsManager = new AssetsManager();
			
			configuration = new PlayerConfiguration();
		
			var configurationXMLLoader:XMLFileLoader = new XMLFileLoader();
			var configurationLoader:ConfigurationLoader = new ConfigurationLoader(configurationXMLLoader);			
			configurationLoader.addEventListener(Event.COMPLETE, onConfigurationReady);			
			configurationLoader.load(parameters, configuration);
			
			function onConfigurationReady(event:Event):void
			{	
				CONFIG::LOGGING
				{
				logger.trackObject("PlayerConfiguration", configuration);	
				var p:uint = 0;
				for each(var pluginResource:MediaResourceBase in configuration.pluginConfigurations)
				{
					logger.trackObject("PluginResource"+(p++), pluginResource);
				}
				}
				if (configuration.skin != null && configuration.skin != "")
				{
					var skinLoader:XMLFileLoader = new XMLFileLoader();
					skinLoader.addEventListener(IOErrorEvent.IO_ERROR, onSkinLoaderFailure);
					skinLoader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onSkinLoaderFailure);
					skinLoader.addEventListener(Event.COMPLETE, onSkinLoaderComplete);
					skinLoader.load(configuration.skin);
				}
				else
				{
					onSkinLoaderComplete();
				}
			}
				
			function onSkinLoaderComplete(event:Event = null):void
			{
				if (event != null)
				{
					var skinLoader:XMLFileLoader = event.target as XMLFileLoader;
					var skinParser:SkinParser = new SkinParser();
					skinParser.parse(skinLoader.xml, assetManager);
				}
				
				var chromeProvider:ChromeProvider = ChromeProvider.getInstance();
				chromeProvider.addEventListener(Event.COMPLETE, onChromeProviderComplete);
				if (chromeProvider.loaded == false && chromeProvider.loading == false)
				{
					chromeProvider.load(assetManager);
				}
				else
				{
					onChromeProviderComplete();
				}
			}
			
			function onSkinLoaderFailure(event:Event):void
			{
				trace("WARNING: failed to load skin file at " + configuration.skin);
				onSkinLoaderComplete();
			}
			
			function onChromeProviderComplete(event:Event = null):void
			{
				initializeControl();			
				initializeView();	
				
				// After initialization, either load the assigned media, or
				// load requested plug-ins first, and then load the assigned
				// media:
				var pluginLoader:PluginLoader = new PluginLoader(configuration.pluginConfigurations, factory);
				pluginLoader.addEventListener(Event.COMPLETE, loadMedia);
				pluginLoader.loadPlugins();
			}			
		}
		
		// Internals
		//
		
		private function initializeControl():void
		{
			// Construct a media factory and add support for playlists:
			factory = new StrobeMediaFactory(configuration);

			// Construct a media controller, and configure it:
			
			player = new StrobeMediaPlayer();
			CONFIG::LOGGING
			{
				player = new DebugStrobeMediaPlayer();
			}
			player.addEventListener(MediaErrorEvent.MEDIA_ERROR, onMediaError);
			player.autoPlay 			= configuration.autoPlay;
			player.loop 				= configuration.loop;
			player.autoSwitchQuality 	= configuration.autoSwitchQuality;	
			player.videoRenderingMode	= configuration.videoRenderingMode;
			player.highQualityThreshold	= configuration.highQualityThreshold;	
		}
		
		private function initializeView():void
		{			
			// Set the SWF scale mode, and listen to the stage change
			// dimensions:
			_stage.scaleMode = StageScaleMode.NO_SCALE;
			_stage.align = StageAlign.TOP_LEFT;
			_stage.addEventListener(Event.RESIZE, onStageResize);
			
			mainContainer = new MediaContainer();
			mainContainer.backgroundColor = configuration.backgroundColor;
			mainContainer.backgroundAlpha = 1;
			
			addChild(mainContainer);
			
			mediaContainer = new MediaContainer();
			mediaContainer.clipChildren = true;
			mediaContainer.layoutMetadata.percentWidth = 100;
			mediaContainer.layoutMetadata.percentHeight = 100;
			
			controlBarContainer = new MediaContainer();
			controlBarContainer.layoutMetadata.percentWidth = 100;
			controlBarContainer.layoutMetadata.verticalAlign = VerticalAlign.TOP;
			controlBarContainer.layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
			
			// Setup play button overlay:
			if (configuration.playButtonOverlay == true)
			{
				playOverlay = new PlayButtonOverlay();
				playOverlay.configure(<default/>, ChromeProvider.getInstance().assetManager);
				playOverlay.layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
				playOverlay.layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
				playOverlay.layoutMetadata.index = PLAY_OVERLAY_INDEX;
				playOverlay.fadeSteps = OVERLAY_FADE_STEPS;
				mediaContainer.layoutRenderer.addTarget(playOverlay);
			}
			
			// Setup buffer overlay:
			if (configuration.bufferingOverlay == true)
			{
				bufferingOverlay = new BufferingOverlay();
				bufferingOverlay.configure(<default/>, ChromeProvider.getInstance().assetManager);
				bufferingOverlay.layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
				bufferingOverlay.layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
				bufferingOverlay.layoutMetadata.index = BUFFERING_OVERLAY_INDEX;
				bufferingOverlay.fadeSteps = OVERLAY_FADE_STEPS;
				mediaContainer.layoutRenderer.addTarget(bufferingOverlay);
			}
				
			// Setup alert dialog:
			alert = new AlertDialogElement();
			alert.tintColor = configuration.tintColor;				
			
			// Setup authentication dialog:
			loginWindow = new AuthenticationDialogElement();
			loginWindow.tintColor = configuration.tintColor;
			
			loginWindowContainer = new MediaContainer();
			loginWindowContainer.layoutMetadata.index = ALWAYS_ON_TOP;
			loginWindowContainer.layoutMetadata.percentWidth = 100;
			loginWindowContainer.layoutMetadata.percentHeight = 100;
			loginWindowContainer.layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
			loginWindowContainer.layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
			
			loginWindowContainer.addMediaElement(loginWindow);
			
			if (configuration.controlBarMode == ControlBarMode.NONE)
			{
				mainContainer.layoutMetadata.layoutMode = LayoutMode.NONE;
			}
			else
			{
				// Setup control bar:
				controlBar = new ControlBarElement();
				controlBar.autoHide = configuration.controlBarAutoHide;
				controlBar.tintColor = configuration.tintColor;
				
				layout();
				
				controlBarContainer.layoutMetadata.height = controlBar.height;
				controlBarContainer.addMediaElement(controlBar);
				controlBarContainer.addEventListener(WidgetEvent.REQUEST_FULL_SCREEN, onFullScreenRequest);
				
				mainContainer.layoutRenderer.addTarget(controlBarContainer);
				mediaContainer.layoutRenderer.addTarget(loginWindowContainer);
			}			
			
			mainContainer.layoutRenderer.addTarget(mediaContainer);
			
			// Simulate the stage resizing, to update the dimensions of the container:
			onStageResize();
		}
		
		/**
		 * Loads the media or displays an error message on fail.
		 */ 
		private function loadMedia(..._):void
		{
			// Try to load the URL set on the configuration:
			var resource:StreamingURLResource = new StreamingURLResource(configuration.src);
			resource.streamType = configuration.streamType;
			resource.urlIncludesFMSApplicationInstance = configuration.urlIncludesFMSApplicationInstance;
			
			// Add the configuration metadata to the resource.
			// Transform the Object to Metadata instance.
			for (var namespace:String in configuration.assetMetadata)
			{
				resource.addMetadataValue(namespace, configuration.assetMetadata[namespace]);
			}
			CONFIG::LOGGING
			{
				logger.trackResource("AssetResource", resource);		
			}
			media = factory.createMediaElement(resource);
			if (_media == null)
			{
				var mediaError:MediaError
					= new MediaError
						( MediaErrorCodes.MEDIA_LOAD_FAILED
						, OSMFStrings.CAPABILITY_NOT_SUPPORTED
						);
					
				player.dispatchEvent
					( new MediaErrorEvent
						( MediaErrorEvent.MEDIA_ERROR
							, false
							, false
							, mediaError
						)
					);
			}
		}
		
		private function processNewMedia(value:MediaElement):MediaElement
		{
			var processedMedia:MediaElement;			
			
			if (value != null)
			{
				processedMedia = value;
				var layoutMetadata:LayoutMetadata = processedMedia.metadata.getValue(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
				if (layoutMetadata == null)
				{
					layoutMetadata = new LayoutMetadata();
					processedMedia.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, layoutMetadata);	
				} 
				
				layoutMetadata.scaleMode = configuration.scaleMode;
				layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
				layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
				layoutMetadata.percentWidth = 100;
				layoutMetadata.percentHeight = 100;
				layoutMetadata.index = 1;  
				
				processPoster();
			}	
			
			return processedMedia;
		}
		
		private function layout():void
		{	
			controlBarContainer.layoutMetadata.index = ON_TOP;

			if	(	controlBar.autoHide == false
				&&	configuration.controlBarMode == ControlBarMode.DOCKED
				)
			{
				// Use a vertical layout:
				mainContainer.layoutMetadata.layoutMode = LayoutMode.VERTICAL;
				mediaContainer.layoutMetadata.index = 1;
			}
			else
			{
				mainContainer.layoutMetadata.layoutMode = LayoutMode.NONE;
				switch(configuration.controlBarMode)
				{
					case ControlBarMode.FLOATING:
						controlBarContainer.layoutMetadata.bottom = POSITION_OVER_OFFSET;
						break;
					case ControlBarMode.DOCKED:
						controlBarContainer.layoutMetadata.bottom = 0;
						break;
				}
			}
		}
		
		private function set media(value:MediaElement):void
		{
			if (value != _media)
			{
				// Remove the current media from the container:
				if (_media)
				{
					mediaContainer.removeMediaElement(_media);
				}
				
				var processedNewValue:MediaElement = processNewMedia(value);
				if (processedNewValue)
				{
					value = processedNewValue;
				}
				
				// Set the new main media element:
				_media = player.media = value;
				
				if (_media)
				{										
					// Add the media to the media container:
					mediaContainer.addMediaElement(_media);
					
					// Forward a reference to controlBar:
					if (controlBar != null)
					{
						controlBar.target = _media;
					}
					
					// Forward a reference to the play overlay:
					if (playOverlay != null)
					{
						playOverlay.media = _media;
					}
					
					// Forward a reference to the buffering overlay:
					if (bufferingOverlay != null)
					{
						bufferingOverlay.media = _media;
					}
					
					// Forward a reference to login window:
					if (loginWindow != null)
					{
						loginWindow.target = _media;
					}
					
					_stage.addEventListener(FullScreenEvent.FULL_SCREEN, onFullScreen);
					mainContainer.addEventListener(MouseEvent.DOUBLE_CLICK, onFullScreenRequest);
					mediaContainer.doubleClickEnabled = true;
					mainContainer.doubleClickEnabled = true;
				}
				else
				{
					if (playOverlay != null)
					{
						playOverlay.media = null;
					}
					
					if (bufferingOverlay != null)
					{
						bufferingOverlay.media = null;
					}
				}
			}
		}
		
		private function processPoster():void
		{
			// Show a poster if there's one set, and the content is not yet playing back:
			if 	(	configuration
				&&	configuration.poster != null
				&&	configuration.poster != ""
				&&	configuration.autoPlay == false
				&&	player.playing == false
				)
			{
				try
				{
					posterImage = new ImageElement(new URLResource(configuration.poster));
					
					// Setup the poster image:
					posterImage.smoothing = true;
					var layoutMetadata:LayoutMetadata = new LayoutMetadata();
					layoutMetadata.scaleMode = configuration.scaleMode;
					layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
					layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
					layoutMetadata.percentWidth = 100;
					layoutMetadata.percentHeight = 100;
					layoutMetadata.index = POSTER_INDEX;  
					posterImage.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, layoutMetadata);
					LoadTrait(posterImage.getTrait(MediaTraitType.LOAD)).load();
					mediaContainer.addMediaElement(posterImage);
					
					// Listen for the main content player to reach a playing, or playback error
					// state. At that time, we remove the poster:
					player.addEventListener
						( MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE
						, onMediaPlayerStateChange
						);
						
					function onMediaPlayerStateChange(event:MediaPlayerStateChangeEvent):void
					{
						if	(	event.state == MediaPlayerState.PLAYING
							||	event.state == MediaPlayerState.PLAYBACK_ERROR
							)
						{
							// Make sure this event is processed only once:
							player.removeEventListener(event.type, arguments.callee);
							
							// Remove the poster image:
							mediaContainer.removeMediaElement(posterImage);
							LoadTrait(posterImage.getTrait(MediaTraitType.LOAD)).unload();
							posterImage = null;
						}
					}
				}
				catch (error:Error)
				{
					// Fail poster loading silently:
					trace("WARNING: poster image failed to load at", configuration.poster);
				}
			}
		}

		// Handlers
		//
		
		private function onAddedToStage(event:Event):void
		{
			removeEventListener(Event.ADDED_TO_STAGE, onAddedToStage);		
			initialize(loaderInfo.parameters, stage);
		}
	
		/**
		 * Toggles full screen state.
		 */ 
		private function onFullScreenRequest(event:Event=null):void
		{
			if (_stage.displayState == StageDisplayState.NORMAL) 
			{				
				removeChild(mainContainer);
				_stage.fullScreenSourceRect = player.getFullScreenSourceRect(_stage.fullScreenWidth, _stage.fullScreenHeight);
				CONFIG::LOGGING
				{	
					if (_stage.fullScreenSourceRect != null)
					{
						logger.info("Setting fullScreenSourceRect = {0}", _stage.fullScreenSourceRect.toString());
					}
					else
					{
						logger.info("fullScreenSourceRect not set.");
					}
					if (_stage.fullScreenSourceRect !=null)
					{
						logger.qos.rendering.fullScreenSourceRect = 
							_stage.fullScreenSourceRect.toString();
						logger.qos.rendering.fullScreenSourceRectAspectRatio = _stage.fullScreenSourceRect.width / _stage.fullScreenSourceRect.height;
					}
					else
					{
						logger.qos.rendering.fullScreenSourceRect =	"";
						logger.qos.rendering.fullScreenSourceRectAspectRatio = NaN;
					}
					logger.qos.rendering.screenWidth = _stage.fullScreenWidth;
					logger.qos.rendering.screenHeight = _stage.fullScreenHeight;
					logger.qos.rendering.screenAspectRatio = logger.qos.rendering.screenWidth  / logger.qos.rendering.screenHeight;
				}
				
				try
				{
					_stage.displayState = StageDisplayState.FULL_SCREEN;
				}
				catch (error:SecurityError)
				{
					CONFIG::LOGGING
					{	
						logger.info("Failed to go to FullScreen. Check if allowfullscreen is set to false in HTML page.");
					}
					// This exception is thrown when the allowfullscreen is set to false in HTML
					addChild(mainContainer);	
					mainContainer.validateNow();
				}
			}
			else				
			{
				_stage.displayState = StageDisplayState.NORMAL;
			}			
		}
		
		/**
		 * FullScreen state changed handler.
		 */ 
		private function onFullScreen(event:FullScreenEvent):void
		{	
			var scaleFactor:Number;
			
			if (_stage.displayState == StageDisplayState.NORMAL) 
			{		
				if (controlBar)
				{										
					// Set the autoHide property to the value set by the user.
					// If the autoHide property changed we need to adjust the layout settings
					if (controlBar.autoHide!=configuration.controlBarAutoHide)
					{
						controlBar.autoHide = configuration.controlBarAutoHide;	
						layout();
					}
				}
				Mouse.show();	
			}
			else if (_stage.displayState == StageDisplayState.FULL_SCREEN)
			{	
				if (controlBar)
				{
					// We force the autohide of the controlBar in fullscreen
					controlBarWidth = controlBar.width;
					controlBarHeight = controlBar.height;
					
					controlBar.autoHide = true;		
					// If the autoHide property changed we need to adjust the layout settings					
					if (controlBar.autoHide!=configuration.controlBarAutoHide)
					{
						layout();
					}
				}
				addChild(mainContainer);	
				mainContainer.validateNow();			
			}
			
		}
		
		private function onStageResize(event:Event = null):void
		{
			// Propagate dimensions to the main container:
			mainContainer.width = _stage.stageWidth;
			mainContainer.height = _stage.stageHeight;
			
			// Propagate dimensions to the control bar:
			if (controlBar != null)
			{
				if	(	configuration.controlBarMode != ControlBarMode.FLOATING
					||	controlBar.width > _stage.stageWidth
					||	_stage.stageWidth < MAX_OVER_WIDTH
					)
				{
					controlBar.width = _stage.stageWidth;
				}
				else if (configuration.controlBarMode == ControlBarMode.FLOATING)
				{
					controlBar.width = MAX_OVER_WIDTH;
				}				
			}
		}
		
		private function onUncaughtError(event:Event):void
		{
			var timer:Timer = new Timer(3000, 1);
			
			var mediaError:MediaError
				= new MediaError
					( StrobePlayerErrorCodes.CONFIGURATION_LOAD_ERROR
					, StrobePlayerStrings.CONFIGURATION_LOAD_ERROR
					);
			
			timer.addEventListener
				( 	TimerEvent.TIMER 
				,	function(event:Event):void
					{
						onMediaError
							( new MediaErrorEvent
								( MediaErrorEvent.MEDIA_ERROR
								, false
								, false
								, mediaError
								)
							);
					}
			);
			timer.start();
		}
		
		private function onMediaError(event:MediaErrorEvent):void
		{
			// Make sure this event gets handled only once:
			player.removeEventListener(MediaErrorEvent.MEDIA_ERROR, onMediaError);
			
			// Reset the current media:
			player.media = null;
			media = null;
			
			// Forward the raw error message to JavaScript:
			if (ExternalInterface.available)
			{
				try
				{	
					ExternalInterface.call
						( EXTERNAL_INTERFACE_ERROR_CALL
						, ExternalInterface.objectID
						, event.error.errorID, event.error.message, event.error.detail
						);
				}
				catch(_:Error)
				{
				}
			}
			
			// Translate error message:
			var message:String;
			if (configuration.verbose)
			{
				message = event.error.message + "\n" + event.error.detail;
			}
			else
			{
				message = ErrorTranslator.translate(event.error).message;
			}
			
			CONFIG::FLASH_10_1
			{
				var tokens:Array = Capabilities.version.split(/[\s,]/);
				var flashPlayerMajorVersion:int = parseInt(tokens[1]);
				var flashPlayerMinorVersion:int = parseInt(tokens[2]);
				if (flashPlayerMajorVersion < 10 || (flashPlayerMajorVersion  == 10 && flashPlayerMinorVersion < 1))
				{
					if (configuration.verbose)
					{
						message += "\n\nThe content that you are trying to play requires the latest Flash Player version.\nPlease upgrade and try again.";	
					}
					else
					{
						message = "The content that you are trying to play requires the latest Flash Player version.\nPlease upgrade and try again.";
					}								
				}
			}
			
			// If an alert widget is available, use it. Otherwise, trace the message:
			if (alert)
			{
				if (_media != null && mediaContainer.containsMediaElement(_media))
				{
					mediaContainer.removeMediaElement(_media);
				}
				if (controlBar != null && controlBarContainer.containsMediaElement(controlBar))
				{
					controlBarContainer.removeMediaElement(controlBar);
				}
				if (posterImage && mediaContainer.containsMediaElement(posterImage))
				{
					mediaContainer.removeMediaElement(posterImage);
				}
				if (playOverlay != null && mediaContainer.layoutRenderer.hasTarget(playOverlay))
				{
					mediaContainer.layoutRenderer.removeTarget(playOverlay);
				}
				if (bufferingOverlay != null && mediaContainer.layoutRenderer.hasTarget(bufferingOverlay))
				{
					mediaContainer.layoutRenderer.removeTarget(bufferingOverlay);
				}
				
				mediaContainer.addMediaElement(alert);
				alert.alert("Error", message);
			}
			else
			{
				trace("Error:", message); 
			}
		}
		
		private var _stage:Stage;
		
		private var mainContainer:MediaContainer;
		private var mediaContainer:MediaContainer;
		private var controlBarContainer:MediaContainer;
		private var loginWindowContainer:MediaContainer;
		
		private var _media:MediaElement;
		
		private var configuration:PlayerConfiguration;
		private var controlBar:ControlBarElement;
		private var alert:AlertDialogElement;
		private var loginWindow:AuthenticationDialogElement;
		private var posterImage:ImageElement;
		private var playOverlay:PlayButtonOverlay;
		private var bufferingOverlay:BufferingOverlay;
		
		private var controlBarWidth:Number;
		private var controlBarHeight:Number;
		
		/* static */
		private static const ALWAYS_ON_TOP:int = 9999;
		private static const ON_TOP:int = 9998;
		private static const POSITION_OVER_OFFSET:int = 20;
		private static const MAX_OVER_WIDTH:int = 400;
		private static const POSTER_INDEX:int = 2;
		private static const PLAY_OVERLAY_INDEX:int = 3;
		private static const BUFFERING_OVERLAY_INDEX:int = 4;
		private static const OVERLAY_FADE_STEPS:int = 6;
		
		private static const EXTERNAL_INTERFACE_ERROR_CALL:String
		 	= "function(playerId, code, message, detail)"
			+ "{"
			+ "	if (onMediaPlaybackError != null)"
			+ "		onMediaPlaybackError(playerId, code, message, detail);"
			+ "}";
		
		CONFIG::LOGGING
		{
			protected var logger:StrobeLogger = Log.getLogger("StrobeMediaPlayback") as StrobeLogger;
		}
	}
}                    
