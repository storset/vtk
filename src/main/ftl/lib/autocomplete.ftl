<#--
  - File: autocomplete.ftl
  - 
  - Description: Macros for setting up autocomplete
  - 
  - Required model data:
  -  
  - Optional model data:
  -   
  -->

<#macro addAutoCompleteScripts srcBase>
  <link type="text/css" rel="stylesheet" href="${srcBase}/build/autocomplete/assets/skins/sam/autocomplete.css">
  <script type="text/javascript" src="${srcBase}/build/yahoo-dom-event/yahoo-dom-event.js"></script>
  <script type="text/javascript" src="${srcBase}/build/get/get-min.js"></script>
  <script type="text/javascript" src="${srcBase}/build/connection/connection-min.js"></script>
  <script type="text/javascript" src="${srcBase}/build/animation/animation-min.js"></script>
  <script type="text/javascript" src="${srcBase}/build/json/json-min.js"></script>
  <script type="text/javascript" src="${srcBase}/build/autocomplete/autocomplete-min.js"></script>
</#macro>

<#macro prepareAutoCompleteInputField fieldName value>
  <#-- div class="yui-skin-sam" --> 
    <div id="${fieldName}.autoComplete">
      <input type="text" id="resource.${fieldName}" name="resource.${fieldName}" value="${value?html}" size="32" />
      <div id="${fieldName}.autoCompleteContainer"></div>
    </div>
    <script type="text/javascript">
    <!--
      var dataSource = new YAHOO.widget.DS_ScriptNode("/?vrtx=admin&action=autocomplete&field=${fieldName}", ["${fieldName}"]);
      dataSource.scriptQueryParam = "${fieldName}"; 
      var autoComplete = new YAHOO.widget.AutoComplete("resource.${fieldName}", "${fieldName}.autoCompleteContainer", dataSource);
    //--> 
    </script>
  <#-- /div -->
</#macro>