package org.osmf.player.chrome.widgets
{
	import flash.display.Sprite;
	
	import flexunit.framework.Assert;
	
	import org.flexunit.asserts.assertEquals;
	import org.flexunit.asserts.assertNotNull;
	import org.osmf.player.chrome.assets.FontAsset;
	import org.osmf.player.chrome.assets.FontResource;
	
	public class TestTimeHintWidget
	{		
		[Before]
		public function setUp():void
		{
			timeHintWidget = new TimeHintWidget();
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
		public function testGetEmpty_text():void
		{
			assertEquals("", timeHintWidget.text)
		}
		
		[Test]
		public function testGetSet_text():void
		{
			timeHintWidget.text = "00:02";
			assertEquals("00:02", timeHintWidget.text);
		}
		
		[Test]
		public function testSameText_text():void
		{
			timeHintWidget.text = "some text";
			timeHintWidget.text = "some text";
			assertEquals("some text", timeHintWidget.text);			
		}
		
		[Test]
		public function testTimeHintWidget():void
		{
			assertNotNull(timeHintWidget);
		}
		
		private var timeHintWidget:TimeHintWidget;

	}
}