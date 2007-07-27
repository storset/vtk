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

        <!--<p>O: Umodifisert FCKeditor</p>
        Config: 
        <pre>
          fckeditorBase: ${fckeditorBase.url?html}
          fckSource.getURL: ${fckSource.getURL?html}
          fckSource.putURL: ${fckSource.putURL?html}
        </pre>-->

        
	<form action="JavaScript: performSave();">
	        <div id="myEditorDiv">hello</div>
	</form>

        <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
	<script type="text/javascript">
		createFCKEditorInDiv("myEditorDiv", 700, 500, "myEditorIstance"); 
	
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
	
			fck.Config['FullPage'] = true;
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
        
    </body>
</html>
