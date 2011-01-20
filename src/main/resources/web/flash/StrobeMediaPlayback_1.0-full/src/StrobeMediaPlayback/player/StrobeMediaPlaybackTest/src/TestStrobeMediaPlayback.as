package
{
	import flexunit.framework.Assert;
	
	import mx.core.Application;
	import mx.core.FlexGlobals;
	
	import org.flexunit.asserts.assertNotNull;
	import org.flexunit.asserts.assertStrictlyEquals;
	import org.flexunit.asserts.fail;
	
	public class TestStrobeMediaPlayback
	{		
		[Before]
		public function setUp():void
		{
			player = new StrobeMediaPlayback();
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
		public function testAddToStage():void
		{
			player.name = "player";
			FlexGlobals.topLevelApplication.stage.addChild(player);
			
			assertStrictlyEquals(player, FlexGlobals.topLevelApplication.stage.getChildByName("player"));
		}

		
		[Test]
		public function testInitialize():void
		{
			try
			{
				player.initialize(PARAMS_ONLY_VIDEO, FlexGlobals.topLevelApplication.stage);
			}
			catch (e:Error)
			{
				fail(e.message);
			}

		}
		
		[Test]
		public function testInitializeWithPlugins():void
		{
			try
			{
				player.initialize(PARAMS_SINGLE_PLUGIN, FlexGlobals.topLevelApplication.stage);
			}
			catch (e:Error)
			{
				fail(e.message);
			}
		}
		
		[Test]
		public function testStrobeMediaPlayback():void
		{
			assertNotNull(player);
		}

		private var player:StrobeMediaPlayback;	
		
		/* static */
		
		private static const STAGE_INDEX:uint = 53;
		
		private static const PARAMS_ONLY_VIDEO:Object 
			= { src: "http://media.url/movie.flv" } 
			
		private static const PARAMS_SINGLE_PLUGIN:Object = 
			{ src: "http://media.url/movie.flv"
			, plugin_dummy: "http://path.to/dummy/plugin.swf"
			} 

	}
}