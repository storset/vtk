package org.osmf.player.debug
{
	import flash.events.Event;
	import flash.events.StatusEvent;
	import flash.events.TimerEvent;
	import flash.external.ExternalInterface;
	import flash.net.LocalConnection;
	import flash.net.getClassByAlias;
	import flash.utils.Dictionary;
	import flash.utils.Timer;
	import flash.utils.getQualifiedClassName;
	
	import org.osmf.player.chrome.configuration.ConfigurationUtils;
	import org.osmf.logging.TraceLogger;
	import org.osmf.media.MediaResourceBase;
	import org.osmf.metadata.Metadata;
	import org.osmf.player.debug.qos.QoSDashboard;

	/**
	 * StrobeLogger extends the OSMF logging framework by providing custom 
	 * utility methods which can be used for tracking qos related measures, metrics 
	 * or indicators as well as any custom object.
	 */ 
	public class StrobeLogger extends TraceLogger
	{
		/**
		 * Constructor.
		 * 
		 * @param category The category value for the Logger.
		 **/
		public function StrobeLogger(category:String, logHandler:LogHandler)
		{
			super(category);	
			this.logHandler = logHandler;
		}
		
		public function get qos():QoSDashboard
		{
			return logHandler.qos;
		}
		
		public function trackProperty(namespace:String, key:String, value:Object):void
		{
			logHandler.handleProperty(namespace + "__" + key, value);
		}
		
		public function trackObject(id:String, object:Object, prefix:String=""):void
		{			
			var dyn:Boolean = false;
			for (var key:String in object)
			{
				dyn = true;
				var value:Object = object[key];
				if (!(value is Array 					
					|| getQualifiedClassName(value).indexOf("::") > 0))
				{
					if (getQualifiedClassName(value) == "Object")
					{
						trackObject(id, value, prefix + key + ".");
					}
					else
					{
						trackProperty(id, prefix+key, value);
					}
				}
			}
			
			if (!dyn)
			{
				var fields:Dictionary = ConfigurationUtils.retrieveFields(object, false);			
				for (var f:String in fields)
				{
					try{
						
						if(fields[f].indexOf("::") < 0 
							&& fields[f].indexOf("Object") < 0
							&& fields[f].indexOf("Array") < 0
						)
						{
							trackProperty(id, f, object[f]);
						}
					}
					catch(ignore:Error)
					{
						//trace(ignore.message);
					}
				}
				
				if (object is MediaResourceBase)
				{
					trackResource(id, object as MediaResourceBase);
				}
			}
		}
		
		public function trackResource(id:String, resource:MediaResourceBase):void
		{
			for each (var ns:String in resource.metadataNamespaceURLs)
			{
				var value:Object = resource.getMetadataValue(ns);
				if (isPrimitive(value))
				{
					trackProperty(id, "metadata[" + ns + "]", value);
				}
				else if (value is Metadata)
				{
					trackMetadata(id, "metadata[" + ns + "]", value as Metadata);
				}
				else
				{
					trackObject(id, value, "metadata[" + ns + "]");
				}
			}
		}
		
		public function event(event:Event):void
		{	
			trackObject("StrobeMediaPlayer", event.target);
			
			var msg:String = getQualifiedClassName(event);
			msg += " (";
			var fields:Dictionary = ConfigurationUtils.retrieveFields(event, false);
			for (var f:String in fields)
			{
				try
				{
					if (f != "cancelable" 
						&& f != "bubbles"
						&& f != "eventPhase")
					{
						if(fields[f].indexOf("::") < 0 
							&& fields[f].indexOf("Object") < 0
							&& fields[f].indexOf("Array") < 0
						)
						{
							msg += f + ":" + event[f] + "  ";
						}
					}
				}
				catch(ignore:Error){}
			}
			msg += ")";
			logMessage("EVENT", msg, new Array());
		}

		/**
		 * This function does the actual logging - sending the message to the debug 
		 * console using the trace statement. It also applies the parameters, if any, 
		 * to the message string.
		 *  
		 *  @langversion 3.0
		 *  @playerversion Flash 10
		 *  @playerversion AIR 1.5
		 *  @productversion OSMF 1.0
		 */		 
		override protected function logMessage(level:String, message:String, params:Array):void
		{
			var logMessage:LogMessage = new LogMessage(level, category, message, params);
			logHandler.handleLogMessage(logMessage);
		}	
		
		
		// Internals
		// 		
		private function trackMetadata(id:String, ns:String, metadata:Metadata):void
		{
			for each (var key:String in metadata.keys)
			{
				var value:Object = metadata.getValue(key);
				if (isPrimitive(value))
				{
					trackProperty(id, ns + "." + key, value);
				}
				else
				{
					trackObject(id, value, ns+".");
				}
			}
		}
		
		private function isPrimitive(value:Object):Boolean
		{
			return value is String || value is Number || value is Boolean || value is uint || value is int; 
		}
		
		private var logHandler:LogHandler;
	}
}