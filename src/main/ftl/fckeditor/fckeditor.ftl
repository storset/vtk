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
        <!--
        <script type="text/javascript" src="a.js"></script>
        <script type="text/javascript" src="../../fckeditor/fckeditor.js"></script>
        -->
    </head>
    <body>

        <p>A: Editor for HTML 1 Transitional</p>
        Config: 
        <pre>
          fckeditorBase: ${fckeditorBase.url?html}
          fckSource.getURL: ${fckSource.getURL?html}
          fckSource.putURL: ${fckSource.putURL?html}
        </pre>


        <!--
        <div id="myEditorDiv">hello</div>
	<script type="text/javascript">
		createFCKEditorInDiv("myEditorDiv", testhtml, 700, 500, "myEditorIstance");
	</script>
        -->
    </body>
</html>
