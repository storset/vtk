package org.osmf.player.elements.playlistClasses
{
	import org.osmf.media.MediaFactory;
	import org.osmf.traits.LoadState;
	import org.osmf.traits.LoadTrait;
	
	public class MockPlaylistLoader extends PlaylistLoader
	{
		public function MockPlaylistLoader(factory:MediaFactory=null, resourceConstructorFunction:Function=null)
		{
			super(factory, resourceConstructorFunction);
		}
		
		public function set playlistContent(value:String):void
		{
			_playlistContent = value;
		}
		
		override protected function executeLoad(loadTrait:LoadTrait):void 
		{
			updateLoadTrait(loadTrait, LoadState.LOADING);
			processPlaylistContent(_playlistContent, loadTrait);
		}
		
		// Internals
		//
		
		private var _playlistContent:String;
	}
}