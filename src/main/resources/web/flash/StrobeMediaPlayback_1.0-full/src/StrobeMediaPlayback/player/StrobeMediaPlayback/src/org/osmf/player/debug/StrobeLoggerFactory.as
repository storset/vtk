package org.osmf.player.debug
{
	import flash.utils.Dictionary;
	
	import org.osmf.logging.Logger;
	import org.osmf.logging.LoggerFactory;

	/**
	 * StrobeLoggerFactory is needed for hooking into the OSMF logging framework.
	 */ 
	public class StrobeLoggerFactory extends LoggerFactory
	{
		public function StrobeLoggerFactory(logHandler:LogHandler)
		{
			loggers = new Dictionary();
			this.logHandler = logHandler;
		}
		
		/**
		 * @inheritDoc
		 *  
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion OSMF 1.0
		 */
		override public function getLogger(name:String):Logger
		{
			var logger:Logger = loggers[name];
			
			if (logger == null)
			{
				logger = new StrobeLogger(name, logHandler);
				loggers[name] = logger;
			}
			
			return logger;
		}
		
		// internal
		//
		
		private var loggers:Dictionary;	
		private var logHandler:LogHandler;
	}
}