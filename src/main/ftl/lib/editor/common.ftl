<#ftl strip_whitespace=true>
<#--
  - File: common.ftl
  -
  - Add common JS old and new editor.ftl
  -
  -->
  
<#import "../vortikal.ftl" as vrtx />

<#macro addCkScripts>
  <link rel="stylesheet" type="text/css" href="${webResources?html}/jquery/plugins/jquery.autocomplete.css" />
  <link rel="stylesheet" type="text/css" href="${webResources?html}/js/autocomplete/autocomplete.override.css" />
  <script type="text/javascript" src="${fckeditorBase.url?html}/ckeditor.js"></script>
  <script type="text/javascript" src="${webResources?html}/js/plugins/mustache.js"></script>
  <script type="text/javascript" src="${jsBaseURL?html}/editor.js"></script>
  <script type="text/javascript" src="${fckeditorBase.url?html}/adapters/jquery.js"></script>
  <script type='text/javascript' src='${webResources?html}/jquery/plugins/jquery.autocomplete.js'></script>
  <script type='text/javascript' src='${webResources?html}/js/autocomplete/autocomplete.js'></script>
  <script type='text/javascript' src='${webResources?html}/js/autocomplete/autocomplete-permissions.js'></script>
</#macro>

<#macro addCommonScripts language oldEditor=false>

  <script type="text/javascript"><!--
    var EDITORS_AT_INIT = [];
    
    var datePickerLang = "${language}";
    var tooLongFieldPre = "<@vrtx.msg code='editor.too-long-field-pre' />";
    var tooLongFieldPost = "<@vrtx.msg code='editor.too-long-field-post' />";
         
    // IE CSS
    if (vrtxAdmin.isIE && typeof cssFileList !== "undefined") {
      cssFileList.push("/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css");
    }
  // -->
  </script>

  <!-- JQuery UI (used for datepicker) -->
  <#if language = "no">
    <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-${jQueryUiVersion}.custom/js/jquery.ui.datepicker-no.js"></script>
  <#elseif language = "nn">
    <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-${jQueryUiVersion}.custom/js/jquery.ui.datepicker-nn.js"></script>
  </#if>

  <#if oldEditor>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker/datepicker-admin-old.js"></script>
  <#else>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker/datepicker-admin.js"></script>  
  </#if>
</#macro>

<#macro createEditor content completeEditor=false withoutSubSuper=false simpleHTML=false>
  <script type="text/javascript"><!--
      if (CKEDITOR.env.isCompatible) {
        try {
          if (typeof EDITORS_AT_INIT !== "undefined") {
            EDITORS_AT_INIT.push(['${content}', ${completeEditor?string}, ${withoutSubSuper?string}, 
	                              '${baseFolder?js_string}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	                              '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList, ${simpleHTML?string}]);
	      } else {
	        $(document).ready(function() {
	          vrtxEditor.newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string}, 
	                               '${baseFolder?js_string}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
	                               '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList, ${simpleHTML?string});
	        });
	      }
	    } catch (e) {
	      if(typeof console !== "undefined" && console.log) {
	        console.log(e);
	      }
	    }
      }
  //-->
  </script>
</#macro>