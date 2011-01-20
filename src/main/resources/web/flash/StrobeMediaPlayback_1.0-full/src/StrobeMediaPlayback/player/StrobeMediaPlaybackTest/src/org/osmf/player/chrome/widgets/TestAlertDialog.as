package org.osmf.player.chrome.widgets
{
	import flexunit.framework.Assert;
	
	import org.flexunit.asserts.assertEquals;
	import org.flexunit.asserts.assertFalse;
	import org.flexunit.asserts.assertNotNull;
	import org.flexunit.asserts.assertTrue;
	import org.osmf.player.chrome.assets.AssetsManager;
	
	public class TestAlertDialog
	{		
		[Before]
		public function setUp():void
		{
			alertDialog = new AlertDialog();
			
			var captionLabel:LabelWidget = new LabelWidget();
			captionLabel.id = "captionLabel";
			
			var messageLabel:LabelWidget = new LabelWidget();
			messageLabel.id = "messageLabel";
			
			var closeButton:ButtonWidget = new ButtonWidget();
			closeButton.id = "closeButton";			
			
			alertDialog.addChildWidget(captionLabel);
			alertDialog.addChildWidget(messageLabel);
			alertDialog.addChildWidget(closeButton);
			
			alertDialog.configure(<default />, new AssetsManager());
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
		public function testAlert():void
		{
			alertDialog.alert(CAPTION, MESSAGE);
			
			assertTrue(alertDialog.visible);
		}

		[Test]
		public function testMultipleAlert():void
		{
			alertDialog.alert(CAPTION, MESSAGE);
			alertDialog.alert(CAPTION1, MESSAGE1);
			
			assertTrue(alertDialog.visible);
			assertEquals(CAPTION, (alertDialog.getChildWidget("captionLabel") as LabelWidget).text);
			assertEquals(MESSAGE, (alertDialog.getChildWidget("messageLabel") as LabelWidget).text);
			
			alertDialog.close(false);
			
			assertEquals(CAPTION1, (alertDialog.getChildWidget("captionLabel") as LabelWidget).text);
			assertEquals(MESSAGE1, (alertDialog.getChildWidget("messageLabel") as LabelWidget).text);
		}



		
		[Test]
		public function testAlertDialog():void
		{
			assertFalse(alertDialog.visible);
		}
		
		[Test]
		public function testClose():void
		{
			alertDialog.alert(CAPTION, MESSAGE);
			
			alertDialog.close(false);
			
			assertFalse(alertDialog.visible);
		}
		
		[Test]
		public function testCloseAll():void
		{
			alertDialog.alert(CAPTION, MESSAGE);
			alertDialog.alert(CAPTION1, MESSAGE1);
			
			alertDialog.close(true);
			
			assertFalse(alertDialog.visible);
		}
		
		private var alertDialog:AlertDialog;
		
		/* static */
		private static const CAPTION:String = "Title:";
		private static const CAPTION1:String = "Another Title:";
		private static const MESSAGE:String = "Some sample text.";
		private static const MESSAGE1:String = "Another sample text.";
	}
}