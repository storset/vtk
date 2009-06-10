<#import "/lib/vortikal.ftl" as vrtx />

<#macro setup>
	<script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
	<script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/serverbrowsedialog.js"></script>
	<script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
    <script language="Javascript" type="text/javascript">
      function newEditor(name, completeEditor, withoutSubSuper) {

        var completeEditor = completeEditor != null ? completeEditor : false;
        var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false; 
        
        var fck = new FCKeditor( name ,'100%', 400);
        fck.BasePath = "${fckeditorBase.url?html}/";

        fck.Config['DefaultLanguage'] = '<@vrtx.requestLanguage />';

        fck.Config['CustomConfigurationsPath'] = '${fckeditorBase.url?html}/custom-fckconfig.js';

         if (completeEditor) {
            fck.ToolbarSet = 'Complete'; 
         } else if(withoutSubSuper){
			fck.ToolbarSet = 'Inline-S';
         } else {
            fck.ToolbarSet = 'Inline';
         }
     
         // File browser
         <#if resourceContext.parentURI?exists>
            var baseFolder = "${resourceContext.parentURI?html}";
         <#else>
         	var baseFolder = "/";
         </#if>
         fck.Config['LinkBrowserURL']  = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['ImageBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['FlashBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';

         fck.Config.LinkUpload = false;
         fck.Config.ImageUpload = false;
         fck.Config.FlashUpload = false;

         // Misc setup
         fck.Config['FullPage'] = false;
         fck.Config['ToolbarCanCollapse'] = false;
         fck.Config['TabSpaces'] = 4;

         fck.Config['FontFormats'] = 'p;h2;h3;h4;h5;h6;pre';
       
         fck.Config.EMailProtection = 'none';
         fck.Config.DisableFFTableHandles = false;
	 	 fck.Config.ForcePasteAsPlainText = false;

         fck.Config['SkinPath'] = fck.BasePath + 'editor/skins/silver/';
         fck.Config.BaseHref = '${fckeditorBase.documentURL?html}';

         var cssFileList = new Array(
         <#if fckEditorAreaCSSURL?exists>
           <#list fckEditorAreaCSSURL as cssURL>
             "${cssURL?html}",
           </#list>
         </#if>
         "");

         /* Fix for div contianer display in ie */
         var browser = navigator.userAgent;
         var ieversion = new Number(RegExp.$1)
         if(browser.indexOf("MSIE") > -1 && ieversion <= 7){
           cssFileList[cssFileList.length-1] = "/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css";
         }

         fck.Config['EditorAreaCSS'] = cssFileList;
         fck.ReplaceTextarea();
      }

      function FCKeditor_OnComplete(editorInstance) {
        // Get around bug: http://dev.fckeditor.net/ticket/1482
        editorInstance.ResetIsDirty();
        if ('resource.content' == editorInstance.Name) {
          enableSubmit();
        }
      }

      function disableSubmit() {
        document.getElementById("saveButton").disabled = true;
        document.getElementById("saveAndQuitButton").disabled = true;
        return true;
      }

      function enableSubmit() {
         document.getElementById("saveButton").disabled = false;
         document.getElementById("saveAndQuitButton").disabled = false;
         return true;
      }

  </script>
</#macro>

<#macro insert content completeEditor=false withoutSubSuper=false>
    <script language="Javascript" type="text/javascript">
      newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string});      
    </script>
</#macro>
