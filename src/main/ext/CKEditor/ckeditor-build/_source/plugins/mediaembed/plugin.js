/*
* @example An iframe-based dialog with custom button handling logics.
*/
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
                  onOk : function() {
					  for (var i=0; i<window.frames.length; i++) {
					      if(window.frames[i].name == 'iframeMediaEmbed') {
					        var url = window.frames[i].document.getElementById("txtUrl").value;
							if(url.length > 0) {
							    var content = "${include:media-player url=["+url+"]";			    
							}
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
							var style = '';
							if (height.length > 0 || width.length > 0) {
							    style = style + ' style="';
							    if(height.length > 0) {
								  style = style +  'height: ' + height + ';';
							    }
							    if(width.length > 0) {
								  style = style + ' width: ' + width + ';';
				                }
							    style = style + '"';
							}
							var autoplay = window.frames[i].document.getElementById("chkAutoplay");
							if(autoplay.checked == true) {
							    content = content + " autoplay=[true]";
							}
							var align = window.frames[i].document.getElementById("txtAlign").value;
							console.log(align);
				
							if(content.length>0) {
							    content = content + "}";
							}			
							
							var divClassType = '';
							if(contentType.length > 0 && contentType == "audio/mp3") {
							    divClassType = 'vrtx-media-player-audio';
							}
							else if (url.length > 0 && getExtension(url) == "mp3") {
							    divClassType = 'vrtx-media-player-audio';
							}
							else {
							    divClassType ='vrtx-media-player';
							}
		                 }
		      
		              } 		  
		           final_html = 'MediaEmbedInsertData|---' + escape('<div class="'+divClassType+' '+align+'" '+style+'>'+content+'</div>') + '---|MediaEmbedInsertData';
		           editor.insertHtml(final_html);
		           updated_editor_data = editor.getData();
		           clean_editor_data = updated_editor_data.replace(final_html,'<div class="'+divClassType+' '+align+'" '+style+'>'+content+'</div>');
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
        }
    } );
} )();
