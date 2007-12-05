<#ftl strip_whitespace=true>

<#--
  - File: fckeditor.ftl
  - 
  - Required model data:
  -  
  -  fckeditorBase.url
  -  fckSource.getURL
  -  fckCleanup.url
  -  fckBrowse.url
  -  
  - Optional model data:
  -
  -->
<#import "/lib/ping.ftl" as ping />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/fckeditor/fckeditor-textarea.ftl" as fck />

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
   <head>
      <title>FCKeditor</title>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/editor.css">
      <style type="text/css">
        textarea {
           width: 100%;
           height: 20em;
        }
        iframe {
           height: 40em; 
        } 
        div.htmlTitle { 
           padding-bottom: 1em;
        }

        </style>
      <@ping.ping url=pingURL['url'] interval=300 />
      <@fck.declareEditor />
   </head>
   <body>

   <form action="JavaScript: performSave();">
      <div class="htmlTitle">
      <label for="title"><@vrtx.msg code="fck.documentTitle" default="Document title" /></label> 
      <input type="text" id="title" size="40" />
      </div>
     <textarea id="file-content" name="content" rows="40" cols="80">Loading editor...</textarea>
   </form>

   <script type="text/javascript">
   
      function getXmlHttpRequestObject() {
         try {
            var ret = new ActiveXObject("Microsoft.XMLHTTP");
            return ret;
         } catch(e){
            return new XMLHttpRequest();
         }
      }
      
      var xReq = getXmlHttpRequestObject();
      xReq.open("GET", "${fckSource.getURL}", false);
      xReq.send(null);
      srcxhtml = xReq.responseText;
      document.getElementById("file-content").value = srcxhtml;

      // Title
      document.getElementById("title").value = srcxhtml.substring(srcxhtml.indexOf("<title")+7, srcxhtml.indexOf("</title>"));

      function performSave() {
         var oEditor = FCKeditorAPI.GetInstance('file-content');
         var srcxhtml = oEditor.GetXHTML();
         var title = document.getElementById("title");

         // Title
         srcxhtml = srcxhtml.replace(/<title.*<\/title>/i, "<title>" + title.value + "</title>");
   
         // Save document
         var xReq = getXmlHttpRequestObject();
         xReq.open("PUT", "${fckSource.putURL}", false);
         xReq.send(srcxhtml);

         window.status = 'Document saved';
      }
      
   </script>

   <@fck.editorInTextarea textarea="file-content" fullpage=true toolbar="Vortikal" enableFileBrowsers=true />


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
      var iframeid = "myEditorInstance___Frame";
      
      // value in 'px'
      var geckoOffset = 240;
      var iexplore56offset = 240;
      var iexplore7offset = 235;  // window is slightly smaller due to tabs
      
      
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
                             if (typeof document.documentElement.style.maxHeight != 'undefined') { 
                                var offset = iexplore7offset;
                                var height = parseInt(document.documentElement.clientHeight) - offset;
                              }
                              // Internet Explorer 5 and 6
                              else { 
                                var offset = iexplore56offset;
                                var height = parseInt(document.body.clientHeight) - offset;
                              }

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
