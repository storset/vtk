/*
* @example An iframe-based dialog with custom button handling logics.
*/

var props = {
  "url" : "",
  "width" : 256,
  "height" : 192,
  "autoplay" : "false"
};

/** Get the file extension  */
function getExtension(url) {
    var ext = url.match(/\.(avi|asf|fla|flv|mov|mp3|mp4|m4v|mpg|mpeg|mpv|qt|swf|wma|wmv)$/i);
    if (ext != null && ext.length && ext.length > 0) {
	ext = ext[1];
    } else {
	if (url.contains('youtube.com/')) {
	    ext = 'swf';
	} else {
	    ext = '';
	}
    }
    return ext;
}

( function() {
    CKEDITOR.plugins.add( 'MediaEmbed',
    {
        requires: [ 'iframedialog' ],
        init: function( editor )
        {
           var me = this;
           CKEDITOR.dialog.add( 'MediaEmbedDialog', function (editor)
           {	
              return {
                 title : 'Embed Media Dialog',
                 minWidth : 550,
                 minHeight : 200,
                 contents :
                       [
                          {
                             id : 'iframe',
                             label : 'Embed Media',
                             expand : true,
                             elements :
                                   [
                                      {
						               type : 'html',
						               id : 'pageMediaEmbed',
						               label : 'Embed Media',
						               style : 'width : 100%',
						               html : '<iframe src="'+me.path.toLowerCase()+'dialogs/mediaembed_'+editor.config.language+'.html" frameborder="0" name="iframeMediaEmbed" id="iframeMediaEmbed" allowtransparency="1" style="width:100%;height:250px;margin:0;padding:0;"></iframe>'
						              }
                                   ]
                          }
                       ],
                  onShow : function() {
            	        var check = setInterval(function() {
            	           var theIframe = $("iframe#iframeMediaEmbed");
            	           if(theIframe) {
            	        	   var contents = theIframe.contents();
            	        	   if(contents.find("#chkAutoplay").attr("checked") != "undefined") {
            	        		  contents.find("#txtUrl").val(props.url);
            	        		  contents.find("#txtWidth").val(props.width);
            	        		  contents.find("#txtHeight").val(props.height);
                	              if(props.autoplay == "true") {
                	                contents.find("#chkAutoplay").attr("checked", true);  	
                	              } else {
                	                contents.find("#chkAutoplay").attr("checked", false);  
                	              }
                	              props.url = "";
                	              props.width = 507;
                	              props.height = 322;
                	              props.autoplay = "false";
            	                  clearInterval(check);
            	        	   }
            	           }
            	        }, 50);
                  },
                  onOk : function() {
					  for (var i=0; i<window.frames.length; i++) {
					      if(window.frames[i].name == 'iframeMediaEmbed') {
					        var url = window.frames[i].document.getElementById("txtUrl").value;
							if(url.length > 0) {
							    var content = "${include:media-player url=["+url+"]";			    
							  var contentType = window.frames[i].document.getElementById("txtContentType").value;
							  if(contentType.length > 0) {
							    content = content + " content-type=["+contentType+"]";
							  }
							  var width = window.frames[i].document.getElementById("txtWidth").value;
							  if(width.length > 0) {
							    content = content + " width=["+width+"]";
							  }
							  var height = window.frames[i].document.getElementById("txtHeight").value;
							  if(height.length > 0) {
							    content = content + " height=["+height+"]";
						      }
							/*
							var style = '';
							if (height.length > 0 || width.length > 0) {
							    style = style + ' style="';
							    if(height.length > 0) {
								  style = style +  'height: ' + height + 'px;';
							    }
							    if(width.length > 0) {
								  style = style + ' width: ' + width + 'px;';
				                }
							    style = style + '"';
							}
							*/
							  var autoplay = window.frames[i].document.getElementById("chkAutoplay");
							  if(autoplay.checked == true) {
							    content = content + " autoplay=[true]";
							  }
							  var align = window.frames[i].document.getElementById("txtAlign").value;
				
							  if(content.length>0) {
							    content = content + "}";
							  }			
							
							  var divClassType = '';
							  if(contentType.length > 0 && contentType == "audio/mp3") {
							    divClassType = 'vrtx-media-player vrtx-media-player-audio';
							  }
							  else if (url.length > 0 && getExtension(url) == "mp3") {
							    divClassType = 'vrtx-media-player vrtx-media-player-audio';
							  }
							  else {
							    divClassType ='vrtx-media-player';
							  }
							} else {
							  alert("Du må spesifisere en URL");
							  return false;	
							}
		                 }
		      
		              } 		  
		           var final_html = 'MediaEmbedInsertData|---' + escape('<div class="'+divClassType+' '+align+'">'+content+'</div>') + '---|MediaEmbedInsertData';
		           editor.insertHtml(final_html);
		           var updated_editor_data = editor.getData();
		           var clean_editor_data = updated_editor_data.replace(final_html,'<div class="'+divClassType+' '+align+'">'+content+'</div>');
		           editor.setData(clean_editor_data);
                 }
              };
           } );

            editor.addCommand( 'MediaEmbed', new CKEDITOR.dialogCommand( 'MediaEmbedDialog' ) );

            editor.ui.addButton( 'MediaEmbed',
            {
                label: 'Embed Media',
                command: 'MediaEmbed',
                icon: this.path.toLowerCase() + 'images/icon.gif'
            } );
            
            editor.on( 'doubleclick', function( evt ){
            	        var data = evt.data;
        				var element = data.element;

        				var HTML = element.$.innerHTML;
        				if(HTML.indexOf("include:media-player") == -1) {
        				  return null;
        				}
        				
        				extractMediaPlayerProps(HTML);
        				data.dialog = 'MediaEmbedDialog';
        			});
            
            
            if (editor.addMenuItem) {
            	  // A group menu is required
            	  // order, as second parameter, is not required
            	  editor.addMenuGroup('MediaEmbed');
            	 
            	  // Create a menu item
            	  editor.addMenuItem('MediaEmbedDialog', {
            	    label: 'Mediaegenskaper',
            	    command: 'MediaEmbed',
            	    group: 'MediaEmbed',
            	    icon: this.path.toLowerCase() + 'images/icon.gif'
            	  });
            	}
            	  
            	if (editor.contextMenu) {
            	  editor.contextMenu.addListener(function(element, selection) {
            		var HTML = element.$.innerHTML;
            		if(HTML.indexOf("include:media-player") == -1) {
            		  return null;	
            		}
            		
            		extractMediaPlayerProps(HTML);
            		
            	    return { MediaEmbedDialog: CKEDITOR.TRISTATE_ON };
            	  });
            	}
        }
    } );
    
    function extractMediaPlayerProps(HTML) {
      var regexp = [];
      var HTMLOrig = HTML;
    		
      for(var name in props) {
        regexp = new RegExp('(?:' + name + '=\\[)(.*?)(?=\\])'); // non-capturing group for prop=
    		                                                     // TODO: positive lookbehind (non-capturing)
    	var prop = regexp.exec(HTML);
    	if(prop != null) {
   		  if(prop.length = 2) {
   		    props[name] = prop[1]; // get the capturing group 
   		  }
    	}
    	HTML = HTMLOrig; //TODO: is it possible to avoid this?
      }
    }
} )();