<#ftl strip_whitespace=true>
<#--
  - File: common.ftl
  -
  - Add common JS old and new editor.ftl
  -
  -->
  
<#import "../vortikal.ftl" as vrtx />

<#macro addCkScripts>
  <script type="text/javascript" src="${fckeditorBase.url?html}/ckeditor.js"></script>
  <script type="text/javascript" src="${webResources?html}/js/plugins/mustache.js"></script>
  <script type="text/javascript" src="${jsBaseURL?html}/editor-ck-setup-helper.js"></script>
  <script type="text/javascript" src="${fckeditorBase.url?html}/adapters/jquery.js"></script>
  <link rel="stylesheet" type="text/css" href="${webResources?html}/jquery/plugins/jquery.autocomplete.css" />
  <link rel="stylesheet" type="text/css" href="${webResources?html}/js/autocomplete/autocomplete.override.css" />
  <script type='text/javascript' src='${webResources?html}/jquery/plugins/jquery.autocomplete.js'></script>
  <script type='text/javascript' src='${webResources?html}/js/autocomplete/autocomplete.js'></script>
  <script type='text/javascript' src='${webResources?html}/js/autocomplete/autocomplete-permissions.js'></script>
</#macro>

<#macro addCommonScripts language oldEditor=false>

  <script type="text/javascript"><!--
    var datePickerLang = "${language}";
    var tooLongFieldPre = "<@vrtx.msg code='editor.too-long-field-pre' />";
    var tooLongFieldPost = "<@vrtx.msg code='editor.too-long-field-post' />";
         
    // IE CSS
    if (vrtxAdmin.isIE && vrtxAdmin.browserVersion <= 7 && typeof cssFileList !== "undefined") {
      cssFileList.push("/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css");
    }
  // -->
  </script>

  <!-- JQuery UI (used for datepicker) -->
  <link type="text/css" href="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.19.custom/css/smoothness/jquery-ui-1.8.19.custom.css" rel="stylesheet" />
  <#if language = "no">
    <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.19.custom/js/jquery.ui.datepicker-no.js"></script>
  <#elseif language = "nn">
    <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.19.custom/js/jquery.ui.datepicker-nn.js"></script>
  </#if>

  <#if oldEditor>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker/datepicker-admin-old.js"></script>
  <#else>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker/datepicker-admin.js"></script>  
  </#if>
</#macro>

<#macro createEditor content completeEditor=false withoutSubSuper=false simpleHTML=false>
  <script type="text/javascript"><!--
    $(document).ready(function() {
      if (CKEDITOR.env.isCompatible) {
        try {
          newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string}, 
	                '${baseFolder?js_string}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	                '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList, ${simpleHTML?string});
	    } catch (e) {
	      vrtxAdmin.log({msg: e});
	    }
      }
    });
  //-->
  </script>
</#macro>