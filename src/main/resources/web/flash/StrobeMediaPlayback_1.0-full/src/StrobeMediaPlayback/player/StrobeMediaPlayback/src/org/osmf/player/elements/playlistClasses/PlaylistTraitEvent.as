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

package org.osmf.player.elements.playlistClasses
{
	import flash.events.Event;
	
	/**
	 * Defines the event object and types as used by the custom traits that 
	 * InnerPlaylistElement uses.
	 */	
	internal class PlaylistTraitEvent extends Event
	{
		public static const ACTIVE_ITEM_COMPLETE:String = "lastItemComplete";
		public static const ENABLED_CHANGE:String = "enabledChange";
		
		public function PlaylistTraitEvent(type:String, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);
		}
	}
}