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
	import flash.net.NetConnection;
	import flash.net.NetStream;
	
	import org.osmf.net.rtmpstreaming.DroppedFramesRule;
	import org.osmf.net.rtmpstreaming.InsufficientBandwidthRule;
	import org.osmf.net.rtmpstreaming.InsufficientBufferRule;
	import org.osmf.net.rtmpstreaming.RTMPDynamicStreamingNetLoader;
	import org.osmf.net.rtmpstreaming.RTMPNetStreamMetrics;
	import org.osmf.net.rtmpstreaming.SufficientBandwidthRule;

	/**
	 * PlaybackOptimization adapter for RTMPDynamicStreamingNetLoader. 
	 */ 
	public class RTMPDynamicStreamingNetLoaderAdapter extends RTMPDynamicStreamingNetLoader
	{
		/**
		 * Constructor
		 */ 
		public function RTMPDynamicStreamingNetLoaderAdapter(playbackOptimizationManager:PlaybackOptimizationManager)
		{
			this.playbackOptimizationManager = playbackOptimizationManager;
			super();			
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
			if (dsResource != null)
			{
				var metrics:RTMPNetStreamMetrics = new RTMPNetStreamMetrics(netStream);
				var streamType:String = dsResource.getMetadataValue("streamType") as String;
				return new StrobeNetStreamSwitchManager(connection, netStream, dsResource, metrics, getDefaultSwitchingRules(metrics, streamType));
			}
			return null;
		}

		// Internals
		//			
		private function getDefaultSwitchingRules(metrics:RTMPNetStreamMetrics, streamType:String):Vector.<SwitchingRuleBase>
		{
			var rules:Vector.<SwitchingRuleBase> = new Vector.<SwitchingRuleBase>();			
			
			rules.push(new SufficientBandwidthRule(metrics));
			rules.push(new InsufficientBandwidthRule(metrics));
			rules.push(new DroppedFramesRule(metrics));
			if (streamType != StreamType.LIVE && streamType != StreamType.DVR)
			{
				rules.push(new InsufficientBufferRule(metrics));
			}			
			return rules;
		}
		
		private var playbackOptimizationManager:PlaybackOptimizationManager;
	}
}