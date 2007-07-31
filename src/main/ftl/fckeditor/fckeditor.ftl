<#ftl strip_whitespace=true>

<#--
  - File: fckeditor.ftl
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
        <title>FCKeditor</title>
    </head>
    <body>
	<style>
		#contents {padding: 0px;}
	</style>

	<form action="JavaScript: performSave();">
	        <div id="myEditorDiv">hello</div>
	</form>

        <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
	<script type="text/javascript">
		createFCKEditorInDiv("myEditorDiv", "100%", "100%", "myEditorIstance"); 
	
		function getXmlHttpRequestObject(){
			try{
				var ret = new ActiveXObject("Microsoft.XMLHTTP");
				return ret;
			}catch(e){
				return new XMLHttpRequest();
			}
		}
	
		function createFCKEditorInDiv(editorDiv, w, h, editorinstancename){
			var div = document.getElementById(editorDiv);
			var fck = new FCKeditor(editorinstancename, w, h);
			fck.BasePath = "${fckeditorBase.url?html}/";
	
			var xhtml_contents;
			/* Load XHTML from source document */{
				var xReq = getXmlHttpRequestObject();
				xReq.open("GET", "${fckSource.getURL}", false);
				xReq.send(null);
				xhtml_contents = xReq.responseText;
			}
			fck.Value = xhtml_contents;

			// JSON string
			fck.Config['ToolbarSets'] = "( {'Vortikal' : [\
				['Source','DocProps','-','Save'],\
				['Cut','Copy','Paste','PasteText','PasteWord','-','SpellCheck'],\
				['Undo','Redo','-','Find','Replace','-','SelectAll','RemoveFormat'],\
				'/',\
				['Bold','Italic','Underline','StrikeThrough','-','Subscript','Superscript'],\
				['OrderedList','UnorderedList','-','Outdent','Indent'],\
				['JustifyLeft','JustifyCenter','JustifyRight'],\
				['Link','Unlink','Anchor'],\
				['Image','Flash','Table','Rule','SpecialChar','PageBreak'],\
				['FontFormat'],\
				['TextColor','BGColor'],\
				['FitWindow','-','About']\
			]} )";

			fck.ToolbarSet = "Vortikal";
	
			fck.Config['FullPage'] = true;
			fck.Config['ToolbarCanCollapse'] = false;
			div.innerHTML = fck.CreateHtml();
		}
	
		function performSave(){
			var oEditor = FCKeditorAPI.GetInstance('myEditorIstance');
			var srcxhtml = oEditor.GetXHTML();
	
			// Save document
			var xReq = getXmlHttpRequestObject();
			xReq.open("PUT", "${fckSource.putURL}", false);
			xReq.send(srcxhtml);
		}
	</script>
        



		<!-- FCKeditor resize script -->
		<script type="text/javascript">
		/*******************************************************************************************/
		/* Script to dynamically alter IFRAME height, making FCKeditor textarea fit browser window */
		/*******************************************************************************************/
		
		
		// Initialize resize script:
		resizeEditorIframe();
		
		
		function resizeEditorIframe() {
		    // Gecko-based browsers (Firefox, Mozilla, Netscape, Camino etc.)
		    if( window.addEventListener ) {
		        window.addEventListener( "load", dyniframesize, false );
		        window.addEventListener( "resize", dyniframesize, false );
		    }
		    // Internet Explorer
		    else if( window.attachEvent ) {
		        window.attachEvent( "onload", dyniframesize );
		        window.attachEvent( "onresize", dyniframesize );
		    }
		    else {
		        window.onLoad = dyniframsize;
		        window.onResize = dyniframesize;
		    }
		} // end function
		
		
		// default variables for resize function
		var iframeid = "myEditorIstance___Frame";
		
		// value in 'px'
		var geckoOffset = 320;
		var iexplore56offset = 330;
		var iexplore7offset = 350;  // window is slightly smaller due to tabs
		
		
		function dyniframesize() {
		    if( !is_safari ) {
		        if( document.getElementById ) { //begin resizing iframe procedure
		            var editIframe = document.getElementById(iframeid);
		                        
		            if( editIframe && !window.opera ) {
		                    // Gecko-based browsers (Firefox, Mozilla, Netscape, Camino etc.)
		                    if( editIframe.contentDocument && window.innerHeight ) { 
		                        var height = window.innerHeight - geckoOffset;
		                        editIframe.style.height = height.toString() + "px";
		                    }
		                    // Internet Explorer
		                    else if( editIframe.Document && editIframe.Document.body.clientHeight ) {
		                    	// Internet Explorer 7
		                    	if (typeof document.documentElement.style.maxHeight != 'undefined')  // only implemented in IE7+
		                    		var offset = iexplore7offset;
		                        // Internet Explorer 5 and 6
		                        else
		                        	var offset = iexplore56offset;
		                        var height = parseInt(document.body.clientHeight) - offset;
		                        editIframe.style.height = height.toString() + "px";
		                        // Mulig "document.body.clientHeight" skal brukes for IE4/IE5...?
		                    }
		            }
		        }
		                
		        // FCKeditor iframe handling for non-supported browsers is done by the Vortikal core system
		        // (browser-sniffer script etc)
		    
		    }
		} //end function
		</script>







    </body>
</html>
