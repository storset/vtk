package org.osmf.player.configuration
{		
	import org.flexunit.asserts.assertEquals;
	import org.flexunit.asserts.assertFalse;
	import org.flexunit.asserts.assertNull;
	import org.flexunit.asserts.assertTrue;
	import org.osmf.layout.ScaleMode;
	import org.osmf.media.URLResource;
	import org.osmf.player.configuration.*;
	
	
	public class TestPlayerConfiguration
	{			
		[BeforeClass]
		public static function setUpBeforeClass():void
		{
			defaultPlayerConfiguration = new PlayerConfiguration();
		}
		
		[Before]
		public function setup():void
		{
			playerConfiguration = new PlayerConfiguration();
		}
		[After]
		public function tearDown():void
		{
			parameters = null;
			playerConfiguration = null;
		}
	
		
		
		//------------------------------------- default values
		[Test(description="Test default values for all parameters")]
		public function testDefaults():void
		{
			//test the default value.
			parameters = {
				//empty
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("backgroundColor is not default", defaultPlayerConfiguration.backgroundColor, playerConfiguration.backgroundColor );
			assertEquals("src is not default", defaultPlayerConfiguration.src , playerConfiguration.src );
			assertEquals("autoHideControlBar is not default", defaultPlayerConfiguration.controlBarAutoHide, playerConfiguration.controlBarAutoHide );
			assertEquals("autoSwitchQuality is not default", defaultPlayerConfiguration.autoSwitchQuality , playerConfiguration.autoSwitchQuality );		
			assertEquals("loop is not default", defaultPlayerConfiguration.loop, playerConfiguration.loop );
			assertEquals("autoPlay is not default", defaultPlayerConfiguration.autoPlay , playerConfiguration.autoPlay );
			assertEquals("initialBufferTime is not default", defaultPlayerConfiguration.initialBufferTime, playerConfiguration.initialBufferTime );
			assertEquals("expandedBufferTime is not default", defaultPlayerConfiguration.expandedBufferTime , playerConfiguration.expandedBufferTime );
			assertEquals("scaleMode is not default", defaultPlayerConfiguration.scaleMode, playerConfiguration.scaleMode );
			assertEquals("controlBarPosition is not default", defaultPlayerConfiguration.controlBarMode , playerConfiguration.controlBarMode );
			assertEquals("smoothing is not default", defaultPlayerConfiguration.videoRenderingMode, playerConfiguration.videoRenderingMode );
			
/*			//change to this values when values are established
			assertEquals("backgroundColor is not default", 0, playerConfiguration.backgroundColor );
			assertEquals("src is not default", "" , playerConfiguration.src );
			assertEquals("controlBarAutoHide is not default", true, playerConfiguration.controlBarAutoHide );
			assertEquals("autoSwitchQuality is not default", true , playerConfiguration.autoSwitchQuality );		
			assertEquals("loop is not default", false, playerConfiguration.loop );
			assertEquals("autoPlay is not default", false , playerConfiguration.autoPlay );
			assertEquals("initialBufferTime is not default", 1, playerConfiguration.initialBufferTime );
			assertEquals("expandedBufferTime is not default", 10 , playerConfiguration.expandedBufferTime );
			assertEquals("scaleMode is not default", ScaleMode.LETTERBOX, playerConfiguration.scaleMode );
			assertEquals("controlBarMode is not default", ControlBarPosition.BOTTOM , playerConfiguration.controlBarMode );
			assertEquals("smoothing is not default", Smoothing.AUTO, playerConfiguration.smoothing );
*/						
		}
		
		//------------------------------------- src
		
		[Test(description="Test src settings valid url")]
		public function testSrcSettingsValid():void
		{
			parameters = {
				src:"http://mediapm.edgesuite.net/osmf/content/test/logo_animated.flv?test=a%20%20b&mimi=titi"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("src not correctly set", parameters.src, playerConfiguration.src);			
		}
		
		[Test(description="Test src settings nonvalid url")]
		public function testSrcSettingsInvalid1():void		
		{
			parameters = {
				src:"://http://mediapm.edgesuite.net/osmf/content/test/logo_animated.flv"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("invalid src is set", defaultPlayerConfiguration.src, playerConfiguration.src);			
		}
		
		[Test(description="Test src settings javascript")]
		public function testSrcSettingsJavascript():void		
		{
			parameters = {
				src:"javascript:alert('test')"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("javascript src is set", defaultPlayerConfiguration.src, playerConfiguration.src);			
		}
		
		[Test(description="Test src settings empty url")]
		public function testSrcSettingsInvalid2():void		
		{
			parameters = {
				src:""
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("invalid src is set", defaultPlayerConfiguration.src, playerConfiguration.src);			
		}
		
		[Test(description="Test src settings null url")]
		public function testSrcSettingsInvalid3():void		
		{
			parameters = {
				src:null
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("invalid src is set", defaultPlayerConfiguration.src, playerConfiguration.src);			
		}	
		
		//------------------------------------- scaleMode
		[Test(description="Test scaleMode settings")]
		public function testScaleModeSettings():void
		{
			parameters = {
				scaleMode:ScaleMode.NONE
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct scaleMode parameter not set", ScaleMode.NONE , playerConfiguration.scaleMode);		
			
			parameters = {
				scaleMode:ScaleMode.LETTERBOX
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct scaleMode parameter not set", ScaleMode.LETTERBOX, playerConfiguration.scaleMode);			
			
			parameters = {
				scaleMode:ScaleMode.STRETCH
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct scaleMode parameter not set", ScaleMode.STRETCH, playerConfiguration.scaleMode);			
			
			parameters = {
				scaleMode:ScaleMode.ZOOM
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct scaleMode parameter not set", ScaleMode.ZOOM, playerConfiguration.scaleMode);			
			
		}
		
		[Test(description="Test scaleMode invalid settings")]
		public function testScaleModeSettingsInvalid1():void
			{			
			parameters = {
				scaleMode:"mistyped"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("Invalid scaleMode is set", defaultPlayerConfiguration.scaleMode, playerConfiguration.scaleMode);			
		}
		
		[Test(description="Test scaleMode empty settings")]
		public function testScaleModeSettingsInvalid2():void
		{			
			parameters = {
				scaleMode:""
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("Empty scaleMode is set", defaultPlayerConfiguration.scaleMode, playerConfiguration.scaleMode);			
		}
		
		[Test(description="Test scaleMode null settings")]
		public function testScaleModeSettingsInvalid3():void
		{			
			parameters = {
				scaleMode:null
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("Null scaleMode is set", defaultPlayerConfiguration.scaleMode, playerConfiguration.scaleMode);			
		}
		
		//------------------------------------- controlBarMode
		
		[Test(description="Test controlBarMode settings")]
		public function testControlBarPositionSettings():void
		{
			parameters = {
				controlBarMode:ControlBarMode.DOCKED
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct controlBarMode parameter not set", ControlBarMode.DOCKED, playerConfiguration.controlBarMode);		
						
			parameters = {
				controlBarMode:ControlBarMode.FLOATING
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct controlBarMode parameter not set", ControlBarMode.FLOATING, playerConfiguration.controlBarMode);			
			
			parameters = {
				controlBarMode:ControlBarMode.NONE
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct controlBarMode parameter not set", ControlBarMode.NONE, playerConfiguration.controlBarMode);			
			
		}
		
		[Test(description="Test controlBarMode invalid settings")]
		public function testControlBarModeSettingsInvalid1():void
		{			
			parameters = {
				controlBarMode:"mistyped"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("Invalid controlBarMode is set", defaultPlayerConfiguration.controlBarMode, playerConfiguration.controlBarMode);			
		}
		
		[Test(description="Test controlBarMode empty settings")]
		public function testControlBarModeSettingsInvalid2():void
		{			
			parameters = {
				controlBarMode:""
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("Empty controlBarMode is set", defaultPlayerConfiguration.controlBarMode, playerConfiguration.controlBarMode);			
		}

		[Test(description="Test controlBarMode null settings")]
		public function testControlBarModeSettingsInvalid3():void
		{			
			parameters = {
				controlBarMode:null
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("Null controlBarMode is set", defaultPlayerConfiguration.controlBarMode, playerConfiguration.controlBarMode);			
		}
		
		//------------------------------------- smoothing
		
		[Test(description="Test smoothing settings")]
		public function testSmoothingSettings():void
		{
			parameters = {
				videoRenderingMode:VideoRenderingMode.AUTO
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct smoothing parameter not set", VideoRenderingMode.AUTO, playerConfiguration.videoRenderingMode);		
			
			parameters = {
				videoRenderingMode:VideoRenderingMode.NONE
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct smoothing parameter not set", VideoRenderingMode.NONE, playerConfiguration.videoRenderingMode);			
			
			parameters = {
				videoRenderingMode:VideoRenderingMode.SMOOTHING
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("correct smoothing parameter not set", VideoRenderingMode.SMOOTHING, playerConfiguration.videoRenderingMode);			
		}
		
		
		[Test(description="Test smoothing invalid settings")]
		public function testSmoothingInvalid1():void
		{			
			parameters = {
				videoRenderingMode:32
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("Invalid smoothing is set", defaultPlayerConfiguration.videoRenderingMode, playerConfiguration.videoRenderingMode);			
		}	
	
		//------------------------------------- color
		
		//------------------------------------- backgroundColor
		[Test(description="Test backgroundColor settings")]
		public function testColorSetting():void
		{	
			//set the background color
			parameters = {
				backgroundColor:"FF0000" //red
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals(16711680, playerConfiguration.backgroundColor);
			
			//check a short value
			parameters = {
				backgroundColor:"FF" //blue
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("short value not accepted", 255, playerConfiguration.backgroundColor);
			
			//check a base 10 existing value
			parameters = {
				backgroundColor:"10" //blue
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("int value incorrectly processed", 16, playerConfiguration.backgroundColor);
		}
		
		[Test(description="Test backgroundColor invalid settings")]
		public function testColorSettingInvalid1():void
		{	
			//set an invalid value and check that the default is being used.
			parameters = {
				backgroundColor:"mistyped this"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("default backgroundColor not set on non-numbers ",  defaultPlayerConfiguration.backgroundColor, playerConfiguration.backgroundColor);
		}
		
		[Test(description="Test backgroundColor number too big settings")]
		public function testColorSettingInvalid2():void
		{	
			//number too big.
			parameters = {
				backgroundColor:"1000000"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("a number that is not a color code is accepted", defaultPlayerConfiguration.backgroundColor, playerConfiguration.backgroundColor);
		}	
		
		[Test(description="Test backgroundColor # settings")]
		public function testColorSettingHtmlColor():void
		{	
			//set an invalid value and check that the default is being used.
			parameters = {
				backgroundColor:"#FF0000"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("a number that starts with # is not accepted", 0xff0000, playerConfiguration.backgroundColor);
		}	
		
		[Test(description="Test backgroundColor 0x settings")]
		public function testColorSettingHtmlColor2():void
		{	
			//set an invalid value and check that the default is being used.
			parameters = {
				backgroundColor:"0xFF0000"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("a number that starts with 0x is not accepted", 0xff0000, playerConfiguration.backgroundColor);
		}	
	
		[Test(description="Test backgroundColor #0x settings")]
		public function testColorSettingHtmlColor3():void
		{	
			//set an invalid value and check that the default is being used.
			parameters = {
				backgroundColor:"#0xFF0000"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("a number that starts with #0x is accepted", defaultPlayerConfiguration.backgroundColor, playerConfiguration.backgroundColor);
		}	
		
		[Test(description="Test backgroundColor #0x empty settings")]
		public function testColorSettingHtmlColor4():void
		{	
			//set an invalid value and check that the default is being used.
			parameters = {
				backgroundColor:"#"
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("a number that starts containing only # is accepted", defaultPlayerConfiguration.backgroundColor, playerConfiguration.backgroundColor);
		}
		
		[Test(description="Test backgroundColor empty settings")]
		public function testColorSettingInvalid4():void
		{	
			//set an invalid value and check that the default is being used.
			parameters = {
				backgroundColor:""
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("empty string for backgroundColor", defaultPlayerConfiguration.backgroundColor, playerConfiguration.backgroundColor);
		}	
		
		[Test(description="Test backgroundColor null settings")]
		public function testColorSettingInvalid5():void
		{	
			//set an invalid value and check that the default is being used.
			parameters = {
				backgroundColor:null
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("null string for backgroundColor", defaultPlayerConfiguration.backgroundColor, playerConfiguration.backgroundColor);
		}
		
		//------------------------------------- boolean
		[Test(description="Test boolean true settings")]
		public function testBooleanSettingsTrue():void
		{
			parameters = {
				autoPlay:"true", 
				controlBarAutoHide:"True",
				loop:"tRue", 
				autoSwitchQuality:"TRUE"   
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertTrue("true value not set for autoPlay", playerConfiguration.autoPlay);
			assertTrue("true value not set for controlBarAutoHide", playerConfiguration.controlBarAutoHide);
			assertTrue("true value not set for loop", playerConfiguration.loop);			
			assertTrue("true value not set for autoSwitchQuality", playerConfiguration.autoSwitchQuality);
		}
		
		[Test(description="Test boolean false settings")]
		public function testBooleanSettingsFalse():void
		{
			parameters = {
				autoPlay:"false", 
				controlBarAutoHide:"False",
				loop:"fAlse", 
				autoSwitchQuality:"FALSE"   
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertFalse("false value not set for autoPlay", playerConfiguration.autoPlay);
			assertFalse("false value not set for controlBarAutoHide", playerConfiguration.controlBarAutoHide);
			assertFalse("false value not set for loop", playerConfiguration.loop);			
			assertFalse("false value not set for autoSwitchQuality", playerConfiguration.autoSwitchQuality);
		}
		
		[Test(description="Test boolean invalid settings")]
		public function testBooleanSettingsInvalid():void
		{
			parameters = {
				autoPlay:"what3ver", //the default should be used
				controlBarAutoHide:"what3ver", //the default should be used
				loop:"what3ver", //the default should be used
				autoSwitchQuality:"what3ver"   //the default should be used
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("default value not set for autoPlay", defaultPlayerConfiguration.autoPlay, playerConfiguration.autoPlay);
			assertEquals("default value not set for controlBarAutoHide", defaultPlayerConfiguration.controlBarAutoHide, playerConfiguration.controlBarAutoHide);
			assertEquals("default value not set for loop", defaultPlayerConfiguration.loop, playerConfiguration.loop);			
			assertEquals("default value not set for autoSwitchQuality", defaultPlayerConfiguration.autoSwitchQuality, playerConfiguration.autoSwitchQuality);
		}
		
		[Test(description="Test boolean empty settings")]
		public function testBooleanSettingsEmpty():void
		{
			parameters = {
				autoPlay:"", 
				controlBarAutoHide:"",
				loop:"", 
				autoSwitchQuality:""   
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("default value not set for autoPlay", defaultPlayerConfiguration.autoPlay, playerConfiguration.autoPlay);
			assertEquals("default value not set for controlBarAutoHide", defaultPlayerConfiguration.controlBarAutoHide, playerConfiguration.controlBarAutoHide);
			assertEquals("default value not set for loop", defaultPlayerConfiguration.loop, playerConfiguration.loop);			
			assertEquals("default value not set for autoSwitchQuality", defaultPlayerConfiguration.autoSwitchQuality, playerConfiguration.autoSwitchQuality);
		}		
		
		[Test(description="Test boolean null settings")]
		public function testBooleanSettingsNull():void
		{
			parameters = {
				autoPlay:null, 
				controlBarAutoHide:null,
				loop:null, //the default should be used
				autoSwitchQuality:null   //the default should be used
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("default value not set for autoPlay", defaultPlayerConfiguration.autoPlay, playerConfiguration.autoPlay);
			assertEquals("default value not set for controlBarAutoHide", defaultPlayerConfiguration.controlBarAutoHide, playerConfiguration.controlBarAutoHide);
			assertEquals("default value not set for loop", defaultPlayerConfiguration.loop, playerConfiguration.loop);			
			assertEquals("default value not set for autoSwitchQuality", defaultPlayerConfiguration.autoSwitchQuality, playerConfiguration.autoSwitchQuality);
		}
		
		//------------------------------------- numeric
		
		[Test(description="Test numeric settings")]
		public function testNumericSettings():void
		{
			parameters = {
				initialBufferTime:"2.12345678901", 
				expandedBufferTime:"10"			
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("initialBufferTime not set correctly", 2.12345678901, playerConfiguration.initialBufferTime);			
			assertEquals("expandedBufferTime not set correctly", 10, playerConfiguration.expandedBufferTime);
		}
		
		[Test(description="Test numeric invalid settings")]
		public function testNumericSettingsInvalid():void
		{	
			parameters = {
				initialBufferTime:"other", 
				expandedBufferTime:"other"			
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("initialBufferTime not set to default", defaultPlayerConfiguration.initialBufferTime, playerConfiguration.initialBufferTime);			
			assertEquals("expandedBufferTime not set to default", defaultPlayerConfiguration.expandedBufferTime, playerConfiguration.expandedBufferTime);
		}
		
		[Test(description="Test numeric zero settings")]
		public function testNumericSettingsZero():void
		{	
			parameters = {
				initialBufferTime:"0", 
				expandedBufferTime:"0"			
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("initialBufferTime not set to default", 0, playerConfiguration.initialBufferTime);			
			assertEquals("expandedBufferTime not set to default", 0, playerConfiguration.expandedBufferTime);
		}
		
		[Test(description="Test numeric empty settings")]
		public function testNumericSettingsEmpty():void
		{	
			parameters = {
				initialBufferTime:"", 
				expandedBufferTime:""			
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("initialBufferTime not set to default", defaultPlayerConfiguration.initialBufferTime, playerConfiguration.initialBufferTime);			
			assertEquals("expandedBufferTime not set to default", defaultPlayerConfiguration.expandedBufferTime, playerConfiguration.expandedBufferTime);
		}
		
		[Test(description="Test numeric null settings")]
		public function testNumericSettingsNull():void
		{	
			parameters = {
				initialBufferTime:null, 
				expandedBufferTime:null			
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("initialBufferTime not set to default", defaultPlayerConfiguration.initialBufferTime, playerConfiguration.initialBufferTime);			
			assertEquals("expandedBufferTime not set to default", defaultPlayerConfiguration.expandedBufferTime, playerConfiguration.expandedBufferTime);
		}
		
		[Test(description="Test numeric negative settings")]
		public function testNumericSettingsNegative():void
		{	
			parameters = {
				initialBufferTime:"-1.3", 
				expandedBufferTime:"-10"			
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("initialBufferTime not set to default", defaultPlayerConfiguration.initialBufferTime, playerConfiguration.initialBufferTime);			
			assertEquals("expandedBufferTime not set to default", defaultPlayerConfiguration.expandedBufferTime, playerConfiguration.expandedBufferTime);
		}
		
		[Test(description="Test uint settings")]
		public function testUIntSettings():void
		{	
			parameters = {
				highQualityThreshold:"280"		
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("highQualityThreshold not set to 280", parameters.highQualityThreshold, playerConfiguration.highQualityThreshold);
		}
		
		[Test(description="Test uint negative settings")]
		public function testUIntSettingsNegative():void
		{	
			parameters = {
				highQualityThreshold:"-2"		
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("highQualityThreshold not set to default", defaultPlayerConfiguration.highQualityThreshold, playerConfiguration.highQualityThreshold);
		}
		
		[Test(description="Test uint invalid settings")]
		public function testUIntSettingsInvalid():void
		{	
			parameters = {
				highQualityThreshold:"Other"		
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("highQualityThreshold not set to default", defaultPlayerConfiguration.highQualityThreshold, playerConfiguration.highQualityThreshold);
		}
		
		[Test(description="Test uint zero settings")]
		public function testUIntSettingsZero():void
		{	
			parameters = {
				highQualityThreshold:"0"		
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("highQualityThreshold not set to default", 0, playerConfiguration.highQualityThreshold);
		}
		
		[Test(description="Asset metadata")]
		public function testSrcTitle():void
		{	
			parameters = {
				src_title : "title"		
			};			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals(parameters.src_title, playerConfiguration.assetMetadata.title);
		}
		
		
		[Test(description="Asset metadata")]
		public function testSrcOtherNamespace():void
		{   
			parameters = {
				src_title : "title"  ,
				src_namespace : "namespace_a",
				src_namespace_n : "namespace_d",
				src_n_author : "author"
			};           
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			
			assertEquals("invalid title is set", parameters.src_title, playerConfiguration.assetMetadata.namespace_a.getValue("title"));
			assertEquals("invalid author is set", parameters.src_n_author, playerConfiguration.assetMetadata.namespace_d.getValue("author"));
			
		}
		
		[Test(description="Asset metadata")]
		public function testSrcOnlyOtherNamespace():void
		{   
			parameters = {
				src_title : "title"  ,   
				src_namespace_n : "namespace_d",
				src_n_author : "author"
			};           
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);
			assertEquals("invalid title is set", parameters.src_title, playerConfiguration.assetMetadata.title);
			assertEquals("invalid author is set", parameters.src_n_author, playerConfiguration.assetMetadata.namespace_d.getValue("author"));
			
		}
		
		[Test]
		public function testPluginNamedSrc():void
		{
			parameters = {
				plugin_src:"http://lolek.corp.adobe.com/strobe/player/current/ConfigurationEchoPlugin.swf",			
				src_namespace: "PLUGIN_NAMESPACE",
				src_videoName: "Video Name"				
			};			
			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);				
			assertEquals(0, playerConfiguration.pluginConfigurations.length);
			
			assertEquals(parameters.src_videoName, playerConfiguration.assetMetadata["PLUGIN_NAMESPACE"].getValue("videoName"));
		}
		
		[Test]
		public function testLocalRTMP():void
		{
			parameters = {
				src:"rtmp:/vod/sample"	
			};			
			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);				
			
			assertEquals(parameters.src, playerConfiguration.src);
		}
		
		
		[Test]
		public function testLocalRTMPTE():void
		{
			parameters = {
				src:"rtmpte:/vod/sample"	
			};			
			
			new ConfigurationFlashvarsDeserializer().deserialize(parameters, playerConfiguration);				
			
			assertEquals(parameters.src, playerConfiguration.src);
		}
		
		private static var defaultPlayerConfiguration:PlayerConfiguration;
		private var parameters:Object;
		private var playerConfiguration:PlayerConfiguration;
	}
}
