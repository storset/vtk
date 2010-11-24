<#ftl strip_whitespace=true>
<#--
  - File: editor.ftl
  -
  -->
  
<#import "../vortikal.ftl" as vrtx />

<#macro addCkScripts>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/serverbrowsedialog.js"></script>
  <script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/ckeditor.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/admin-fck-setup.js"></script>
  <script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/adapters/jquery.js"></script>
</#macro>

<#macro addDatePickerScripts>
  <!-- JQuery UI (used for datepicker) -->
  <link type="text/css" href="${webResources?html}/jquery-ui-1.7.1.custom/css/smoothness/jquery-ui-1.7.1.custom.css" rel="stylesheet" />
  <script type="text/javascript" src="${webResources?html}/jquery-ui-1.7.1.custom/js/jquery-ui-1.7.1.custom.min.js"></script>
  <script type="text/javascript" src="${webResources?html}/jquery-ui-1.7.1.custom/js/jquery.ui.datepicker-no.js"></script>
  <script type="text/javascript" src="${webResources?html}/jquery-ui-1.7.1.custom/js/jquery.ui.datepicker-nn.js"></script>
  <script type="text/javascript" src="${jsBaseURL?html}/datepicker.js"></script>
</#macro>

<#macro createEditor content completeEditor=false withoutSubSuper=false>
  <script language="Javascript" type="text/javascript"><!--
    var cssFileList = new Array(
      <#if fckEditorAreaCSSURL?exists>
        <#list fckEditorAreaCSSURL as cssURL>
          "${cssURL?html}" <#if cssURL_has_next>,</#if>
        </#list>
     </#if>);

	newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string}, 
	  '${baseFolder?js_string}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	  '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList);

  //-->
  </script>
</#macro>