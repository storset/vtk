<#ftl strip_whitespace=true>
<#--
  - File: editor.ftl
  -
  -->

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