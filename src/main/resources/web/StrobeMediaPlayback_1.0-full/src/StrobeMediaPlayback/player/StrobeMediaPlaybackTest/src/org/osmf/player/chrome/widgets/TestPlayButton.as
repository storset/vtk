package org.osmf.player.chrome.widgets
{
	import flash.events.MouseEvent;
	import flash.ui.Mouse;
	
	import flexunit.framework.Assert;
	
	import org.flexunit.asserts.assertEquals;
	import org.flexunit.asserts.assertFalse;
	import org.flexunit.asserts.assertNotNull;
	import org.flexunit.asserts.assertTrue;
	import org.osmf.MockMediaElement;
	import org.osmf.traits.MediaTraitType;
	import org.osmf.traits.PlayState;
	import org.osmf.traits.PlayTrait;
	
	public class TestPlayButton
	{		
		[Before]
		public function setUp():void
		{
			playableMedia = new MockMediaElement();
			playableMedia.addSomeTrait(new PlayTrait());
			
			unplayableMedia = new MockMediaElement();
			
			playButton = new PlayButton();
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
		public function testPlayButton():void
		{
			assertNotNull(playButton);
		}


		[Test]
		public function testIsVisible():void
		{
			playButton.media = playableMedia;			
			assertTrue(playButton.visible);
		}

		[Test]
		public function testIsNotVisible():void
		{
			playButton.media = unplayableMedia;			
			assertFalse(playButton.visible);
		}
		
		[Test]
		public function testNoMedia():void
		{
			assertTrue(playButton.visible);
		}

		[Test]
		public function testIsPlaying():void
		{
			playButton.media = playableMedia;
			(playableMedia.getTrait(MediaTraitType.PLAY) as PlayTrait).play();
						
			assertFalse(playButton.visible);
		}


		[Test]
		public function testPlayMouseClick():void
		{
			playButton.media = playableMedia;
			playButton.dispatchEvent(new MouseEvent(MouseEvent.CLICK));
			
			assertEquals(PlayState.PLAYING, (playableMedia.getTrait(MediaTraitType.PLAY) as PlayTrait).playState);						
			assertFalse(playButton.visible);
		}
		
		private var playButton:PlayButton;
		
		private var playableMedia:MockMediaElement;
		private var unplayableMedia:MockMediaElement;
	}
}