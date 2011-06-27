<#-- Adds the default required scripts necessary to use show and hide functionality -->
<#import "/lib/vortikal.ftl" as vrtx />

<#macro addMultipleInputFieldsScripts srcBase> 
  <script type="text/javascript" src="${srcBase}/editor-multipleinputfields.js"></script> 
</#macro>

<#macro addMultipleInputFields script>
  loadMultipleInputFields('${script.name}','${vrtx.getMsg("editor.add")}','${vrtx.getMsg("editor.remove")}', '${vrtx.getMsg("editor.move-up")}', '${vrtx.getMsg("editor.move-down")}', '${vrtx.getMsg("editor.browseImages")}');
</#macro>