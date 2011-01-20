package org.osmf.player.chrome.widgets
{
	import flash.display.Loader;
	
	import org.flexunit.asserts.assertFalse;
	import org.flexunit.asserts.assertNotNull;
	import org.flexunit.asserts.assertTrue;
	import org.osmf.MockMediaElement;
	import org.osmf.player.chrome.assets.AssetsManager;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.traits.LoadTrait;
	import org.osmf.traits.LoaderBase;
	import org.osmf.traits.PlayTrait;
	import org.osmf.traits.SeekTrait;
	import org.osmf.traits.TimeTrait;
	import org.osmf.player.chrome.widgets.*;

	public class TestScrubBar
	{		
		[Before]
		public function setUp():void
		{
			media = new MockMediaElement();
			media.addSomeTrait(new LoadTrait(new LoaderBase(), new MediaResourceBase()));
			media.addSomeTrait(new PlayTrait());
			
			timeTrait = new TimeTrait(MEDIA_DURATION);
			seekTrait = new SeekTrait(timeTrait);
			
			scrubBar = new ScrubBar();
			scrubBar.configure(<default/>, new AssetsManager());
			scrubBar.media = media;
		}
		
		[After]
		public function tearDown():void
		{
		}
		
		[BeforeClass]
		public static function setUpBeforeClass():void
		{
		}
		
		[AfterClass]
		public static function tearDownAfterClass():void
		{
		}
		
		[Test] 
		public function testScrubBar():void
		{
			assertNotNull(scrubBar);
		}
		
		[Test]
		public function testScrubBarNotEnabled():void
		{
			assertFalse(scrubBar.enabled);
		}

		[Test]
		public function testScrubBarEnabled():void
		{
			(scrubBar.media as MockMediaElement).addSomeTrait(seekTrait);
			assertTrue(scrubBar.enabled);
		}

		private var scrubBar:ScrubBar;
		private var media:MockMediaElement;
		private var seekTrait:SeekTrait;
		private var timeTrait:TimeTrait;
		
		/* static */
		private static const MEDIA_DURATION:Number = 100;
	}
}