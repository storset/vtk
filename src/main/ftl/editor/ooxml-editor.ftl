<#ftl strip_whitespace=true>
<#--
  - File: ooxml-editor.ftl
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Editor</title>

    <script type="text/javascript" src="${jsBaseURL?html}/plugins/shortcut.js"></script>
    <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />
    <style type="text/css">
      #vrtx-open-webdav-wrapper {
        display: none;
      }
    </style>
    <script type="text/javascript"><!--
      $(function() {
         var agent = navigator.userAgent.toLowerCase();         
         var isWin = ((agent.indexOf("win") != -1) || (agent.indexOf("16bit") != -1));
         var isLinux = (agent.indexOf("linux") != -1);
         var isMac = (agent.indexOf("mac") != -1) && !isWin && !isLinux;
         if(isWin) {
           $("#vrtx-edit-mac").hide(0); 
         } else if(isMac) {
           $("#vrtx-edit-win").hide(0); 
         }
         if(!(isWin || isMac)) {
           $("#vrtx-edit-win-mac").hide(0);
         }
         if ($.browser.msie && $.browser.version >= 5 && isWin) {  
           $("#vrtx-open-webdav-wrapper").show(0);
           $("#vrtx-open-webdav").click(function(e) {
             var openOffice = new ActiveXObject("Sharepoint.OpenDocuments.1").EditDocument(this.href);
             e.stopPropagation();
             e.preventDefault();
           });
           shortcut.add("Ctrl+O",function() {
             $("#vrtx-open-webdav").click();
           });
         }
      });
    // -->
    </script>
  </head>
  <body id="vrtx-ooxml-editor">
    <#assign resourceTypeName = vrtx.resourceTypeName(resourceContext.currentResource) />
    <#assign document>
      <@vrtx.msg code="resourcetype.name.structured-document" />
    </#assign>
    <#assign header>
      <@vrtx.msg code="editor.edit" args=[resourceTypeName?lower_case] /> ${document?lower_case}
    </#assign>
    <h2>${header?html}</h2>
    <div id="vrtx-open-webdav-wrapper">
      <h3>Med Internet Explorer kan du redigere dokumentet direkte</h3>
      <a id="vrtx-open-webdav" class="vrtx-button" href="${webdavUrl?html}"><span>Rediger i ${resourceTypeName}</span></a>
    </div>
    <h3>Steg for steg: Hvordan redigere dokumentet</h3>
    <ol class="vrtx-help-step-by-step">
      <li>Marker og kopier WebDAV-adressen til dokumentet:<br />
         <span class="vrtx-text-grey-bold">${webdavUrl?html}</span>
      </li>
      <li>Start ${resourceTypeName?html}</li>
      <li id="vrtx-edit-win-mac">
          <span id="vrtx-edit-win">På Windows: Velg "Fil" &rarr; "Åpne" (Du kan bruke "Ctrl-O" som snarvei)</span><br />
          <span id="vrtx-edit-mac">På Mac:     Velg "Fil" &rarr; "Åpne URL" (Du kan bruke "Shift-Command-O" som snarvei)</span>
      </li>
      <li>Lim inn den kopierte adressen i feltet "Filnavn" og trykk "Åpne" </li>
    </ol>
  </body>
</html>