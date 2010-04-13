<#-- Adds the default required scripts necessary to use show and hide functionality -->
<#import "/lib/vortikal.ftl" as vrtx />

<#macro addMultipleInputFieldsScripts srcBase> 
  <script type="text/javascript" src="${srcBase}/multipleinputfields.js"></script> 
</#macro>

<#macro addMultipleInputFields script>
      loadMultipleInputFields('${script.name}','${vrtx.getMsg("editor.add")}','${vrtx.getMsg("editor.remove")}');
</#macro>