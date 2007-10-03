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
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
   <head>
      <title>FCKeditor</title>
      <style type="text/css">

       div#contents { 
       margin: 0 !important; padding:0 !important;
       }

       div.activeTab { 
       background-color: #f7f7f7;
       }

       div.tabs ul li.activeTab a, div.tabs ul li.activeTab a:hover {
       background-color: #f7f7f7 !important;
       }

       span.htmlTitle { 
       position:relative;
       left:455px;
       top:-2px;
       }

       .htmlTitlePrefix { 
       font-weight: bold;
       padding-right: 0.5em;
       }

       #myEditorDiv {
       margin-top: -27px;
       padding: 0;
       margin-right: -2px;
       margin-left: -2px;
       margin-bottom: -3px;
       border: 0;
       } 

      </style>
      <@ping.ping url=pingURL['url'] interval=600 />
   </head>
   <body>

   <!--
        <p>${fckeditorBase.url?html}</p>
        <p>${fckSource.getURL}</p>
        <p>${fckCleanup.url?html}</p>
        <p>${fckBrowse.url}</p>
   -->

   <script type="text/javascript" src="${fckeditorBase.url?html}/xmlcleaner.js"></script>

   <!-- div class="fck-fulleditor">
     <span class="htmlTitlePrefix">Tittel:</span><input type="text" id="title" />
   </div -->
   
   <span class="htmlTitle">
     <span class="htmlTitlePrefix">Tittel:</span><input type="text" id="title" />
   </span>

   <form action="JavaScript: performSave();">
      <div id="myEditorDiv">Loading editor...</div>
   </form>

   <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
   <div id="parserDiv" style="height: 0px;" />
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
   
         var srcxhtml;
         /* Load XHTML from source document */{
            var xReq = getXmlHttpRequestObject();
            xReq.open("GET", "${fckSource.getURL}", false);
            xReq.send(null);
            srcxhtml = xReq.responseText;
         }
         fck.Value = srcxhtml;

         // Title
         document.getElementById("title").value = srcxhtml.substring(srcxhtml.indexOf("<title")+7, srcxhtml.indexOf("</title>"));

         /* The toolbar: JSON string
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
         ]} )"; */

         // The toolbar: JSON string
          fck.Config['ToolbarSets'] = "( {'Vortikal' : [\
            ['Save','-','PasteText','PasteWord','-','Undo','Redo','-','Replace','RemoveFormat','-','Link','Unlink','Anchor','Image','Flash','Table','Rule','SpecialChar'],\'/',\
            ['FontFormat','-','Bold','Italic','Underline','StrikeThrough','Subscript','Superscript','OrderedList','UnorderedList','Outdent','Indent','JustifyLeft','JustifyCenter','JustifyRight','TextColor','FitWindow']\
         ]} )";
         fck.ToolbarSet = "Vortikal";

        // File browser
         var baseFolder = "${resourceContext.parentURI?html}";
         fck.Config['LinkBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['ImageBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['FlashBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';

         // Misc setup
         fck.Config['FullPage'] = true;
         fck.Config['ToolbarCanCollapse'] = false;
         fck.Config['FontFormats'] = 'p;h1;h2;h3;h4;h5;h6;pre' ;        

         fck.Config.DisableFFTableHandles = false;
         // fck.Config.FirefoxSpellChecker = true;
	 // fck.Config.BrowserContextMenuOnCtrl = true ;

         //fck.config['BaseHref'] = '/test';

         fck.Config['SkinPath'] = '${fckeditorBase.url?html}/editor/skins/silver/';

         // Create
         div.innerHTML = fck.CreateHtml();
      }

      function performSave(){
         var oEditor = FCKeditorAPI.GetInstance('myEditorIstance');
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
      var geckoOffset = 240;
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
