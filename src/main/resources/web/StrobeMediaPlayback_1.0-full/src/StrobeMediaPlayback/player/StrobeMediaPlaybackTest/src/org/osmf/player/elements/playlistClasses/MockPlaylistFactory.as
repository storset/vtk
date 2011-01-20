package org.osmf.player.elements.playlistClasses
{
	import org.osmf.media.MediaElement;
	import org.osmf.media.MediaFactory;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.media.URLResource;
	
	public class MockPlaylistFactory extends MediaFactory
	{
		public function MockPlaylistFactory()
		{
			constructors = {};
			super();
		}
		
		public function addMediaContstructor(url:String, constructor:Function):void
		{
			constructors[url] = constructor;
		}
		
		override public function createMediaElement(resource:MediaResourceBase):MediaElement
		{
			var result:MediaElement;
			var urlResource:URLResource = resource as URLResource;
			if (urlResource != null)
			{
				var constructor:Function = constructors[urlResource.url];
				if (constructor != null)
				{
					result = constructor(resource);
				}
			}
			return result;
		}
		
		private var constructors:Object;
	}
}