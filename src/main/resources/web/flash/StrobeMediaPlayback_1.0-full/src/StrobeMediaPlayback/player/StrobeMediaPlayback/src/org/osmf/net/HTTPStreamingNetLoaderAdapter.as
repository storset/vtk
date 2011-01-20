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

package org.osmf.net
{
	import flash.events.NetStatusEvent;
	import flash.net.NetConnection;
	import flash.net.NetStream;
	
	import org.osmf.net.httpstreaming.*;
	import org.osmf.net.rtmpstreaming.DroppedFramesRule;

	/**
	 * PlaybackOptimization adapter for RTMPDynamicStreamingNetLoader. 
	 */ 
	public class HTTPStreamingNetLoaderAdapter extends HTTPStreamingNetLoader
	{
		/**
		 * Constructor.
		 */ 
		public function HTTPStreamingNetLoaderAdapter(playbackOptimizationManager:PlaybackOptimizationManager)
		{
			this.playbackOptimizationManager = playbackOptimizationManager;
			super();			
		}
		
		/**
		 * @private
		 *  
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion OSMF 1.0
		 */		
		override protected function processFinishLoading(loadTrait:NetStreamLoadTrait):void
		{		
			super.processFinishLoading(loadTrait);			
		}
		
		/**
		 * @private
		 * 
		 * Overridden to allow the creation of a NetStreamSwitchManager object.
		 *  
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion OSMF 1.0
		 */
		override protected function createNetStreamSwitchManager(connection:NetConnection, netStream:NetStream, dsResource:DynamicStreamingResource):NetStreamSwitchManagerBase
		{
			playbackOptimizationManager.optimizePlayback(netStream, dsResource);
			netStream.addEventListener(NetStatusEvent.NET_STATUS, onNetStatus);
			// Only generate the switching manager if the resource is truly
			// switchable.
			if (dsResource != null)
			{
				var metrics:HTTPNetStreamMetrics = new HTTPNetStreamMetrics(netStream as HTTPNetStream);
				return new StrobeNetStreamSwitchManager(connection, netStream, dsResource, metrics, getDefaultSwitchingRules(metrics));
			}
			return null;
		}
		
		// Internals
		//		
		private function onNetStatus(event:NetStatusEvent):void
		{
			var netStream:NetStream = event.currentTarget as NetStream;
			if (event.info.code == NetStreamCodes.NETSTREAM_BUFFER_EMPTY)
			{
				if (netStream.bufferTime >= 2.0)
				{
					netStream.bufferTime += 1.0;
				}
				else
				{
					netStream.bufferTime = 2.0;
				}						
			}
		}
		
		private function getDefaultSwitchingRules(metrics:HTTPNetStreamMetrics):Vector.<SwitchingRuleBase>
		{
			var rules:Vector.<SwitchingRuleBase> = new Vector.<SwitchingRuleBase>();
			rules.push(new DownloadRatioRule(metrics));
			rules.push(new DroppedFramesRule(metrics));
			return rules;
		}
		private var playbackOptimizationManager:PlaybackOptimizationManager;
	}
}