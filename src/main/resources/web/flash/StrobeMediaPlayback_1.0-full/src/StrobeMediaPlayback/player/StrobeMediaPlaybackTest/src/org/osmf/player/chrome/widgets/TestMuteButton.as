package org.osmf.player.chrome.widgets
{
	import flash.events.Event;
	import flash.events.MouseEvent;
	
	import flexunit.framework.Assert;
	
	import org.flexunit.asserts.assertFalse;
	import org.flexunit.asserts.assertNotNull;
	import org.flexunit.asserts.assertStrictlyEquals;
	import org.flexunit.asserts.assertTrue;
	import org.flexunit.async.Async;
	import org.osmf.MockMediaElement;
	import org.osmf.player.chrome.assets.AssetsManager;
	import org.osmf.events.AudioEvent;
	import org.osmf.traits.AudioTrait;
	import org.osmf.traits.MediaTraitType;
	
	public class TestMuteButton
	{		
		[Before]
		public function setUp():void
		{
			mediaNoAudio = new MockMediaElement();

			mediaLoud = new MockMediaElement();
			mediaLoud.addSomeTrait(new AudioTrait());
			(mediaLoud.getTrait(MediaTraitType.AUDIO) as AudioTrait).volume = 0.9;
			
			mediaMuted = new MockMediaElement();
			mediaMuted.addSomeTrait(new AudioTrait());
			(mediaMuted.getTrait(MediaTraitType.AUDIO) as AudioTrait).muted = true;

			
			muteButton = new MuteButton();
			muteButton.configure(<default/>, new AssetsManager());
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
		public function testMuteButton():void
		{
			assertNotNull(muteButton);
		}
		
		[Test]
		public function testMuteButtonGetSetMedia():void
		{
			muteButton.media = mediaLoud;
			assertStrictlyEquals(mediaLoud, muteButton.media);
		}

		[Test]
		public function testMuteButtonNoAudioTrait():void
		{
			muteButton.media = mediaNoAudio;
			assertFalse(muteButton.visible);
		}
		
		[Test]
		public function testMuteButtonAudioTrait():void
		{
			muteButton.media = mediaLoud;
			assertTrue(muteButton.visible);
		}

		[Test]
		public function testMuteButtonMuteAction():void
		{
			muteButton.media = mediaLoud;
						
			//simulate the click at position (0,0) 
			//since we do not have an actual asset loaded
			muteButton.dispatchEvent(new MouseEvent(MouseEvent.CLICK, false, false, 0, 0));
			
			assertTrue((muteButton.media.getTrait(MediaTraitType.AUDIO) as AudioTrait).muted);
		}


		[Test]
		public function testMuteButtonUnmuteAction():void
		{
			muteButton.media = mediaMuted;
			
			//simulate the click at position (0,0) 
			//since we do not have an actual asset loaded
			muteButton.dispatchEvent(new MouseEvent(MouseEvent.CLICK, false, false, 0, 0));
			
			assertFalse((muteButton.media.getTrait(MediaTraitType.AUDIO) as AudioTrait).muted);
		}
				
		private var muteButton:MuteButton;
		private var mediaNoAudio:MockMediaElement;
		private var mediaLoud:MockMediaElement;
		private var mediaMuted:MockMediaElement;

	}
}