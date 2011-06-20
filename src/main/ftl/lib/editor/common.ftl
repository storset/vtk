<#ftl strip_whitespace=true>
<#--
  - File: editor.ftl
  -
  -->
  
<#import "../vortikal.ftl" as vrtx />

<#macro addCkScripts>
  <script type="text/javascript" src="${jsBaseURL?html}/serverbrowsedialog.js"></script>
  <script type="text/javascript" src="${fckeditorBase.url?html}/ckeditor.js"></script>
  <script type="text/javascript" src="${jsBaseURL?html}/admin-ck-setup.js"></script>
  <script type="text/javascript" src="${fckeditorBase.url?html}/adapters/jquery.js"></script>
</#macro>

<#macro addDatePickerScripts oldEditor=false>
  <!-- JQuery UI (used for datepicker) -->
  <link type="text/css" href="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.8.custom/css/smoothness/jquery-ui-1.8.8.custom.css" rel="stylesheet" />
  <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.8.custom/js/jquery-ui-1.8.8.custom.min.js"></script>
  <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.8.custom/js/jquery.ui.datepicker-no.js"></script>
  <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.8.custom/js/jquery.ui.datepicker-nn.js"></script>
  <#if oldEditor>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker/datepicker-admin-old.js"></script>
  <#else>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker/datepicker-admin.js"></script>  
  </#if>
</#macro>

<#macro createEditor content completeEditor=false withoutSubSuper=false simpleHTML=false>
  <script type="text/javascript"><!--
    var cssFileList = new Array(
      <#if fckEditorAreaCSSURL?exists>
        <#list fckEditorAreaCSSURL as cssURL>
          "${cssURL?html}" <#if cssURL_has_next>,</#if>
        </#list>
     </#if>);

	newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string}, 
	  '${baseFolder?js_string}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	  '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList, ${simpleHTML?string});

  //-->
  </script>
</#macro>
