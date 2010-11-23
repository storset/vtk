<#ftl strip_whitespace=true>
<#--
  - File: editor.ftl
  -
  -->
  
<#import "../vortikal.ftl" as vrtx />

<#macro addFckScripts>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/serverbrowsedialog.js"></script>
  <script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/ckeditor.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/admin-fck-setup.js"></script>
  <script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/adapters/jquery.js"></script>
</#macro>

<#macro createEditor content completeEditor=false withoutSubSuper=false>
  <script language="Javascript" type="text/javascript"><!--
    var cssFileList = new Array(
      <#if fckEditorAreaCSSURL?exists>
        <#list fckEditorAreaCSSURL as cssURL>
          "${cssURL?html}",
        </#list>
     </#if>"");

	newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string}, 
	  '${baseFolder?js_string}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	  '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList);

  //-->
  </script>
</#macro>