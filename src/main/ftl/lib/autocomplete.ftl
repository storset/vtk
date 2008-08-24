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

<#--
 * addAutoCompleteScripts
 *
 * Adds the neccesary scripts for autocomplete.
 *
 * @param srcBase Base src path for scripts
 *
-->
<#macro addAutoCompleteScripts srcBase>
  <link type="text/css" rel="stylesheet" href="${srcBase}/build/autocomplete/assets/skins/sam/autocomplete.css">
  <script type="text/javascript" src="${srcBase}/build/utilities/utilities.js"></script>
  <script type="text/javascript" src="${srcBase}/build/autocomplete/autocomplete-min.js"></script>
  
  <#-- script type="text/javascript" src="${srcBase}/build/yahoo-dom-event/yahoo-dom-event.js"></script>
  <script type="text/javascript" src="${srcBase}/build/get/get-min.js"></script>
  <script type="text/javascript" src="${srcBase}/build/connection/connection-min.js"></script>
  <script type="text/javascript" src="${srcBase}/build/animation/animation-min.js"></script>
  <script type="text/javascript" src="${srcBase}/build/json/json-min.js"></script -->
  
</#macro>

<#--
 * createAutoCompleteInputField
 *
 * Creates an inputfield with added functionality for autocomplete.
 * Example: <@createAutoCompleteInputField fieldName="myInput" value="myInputValue" schema="['dataStructure']"
 *
 * @param fieldName The name of the testfield
 * @param description A description of the fields content
 * @param value The current value in the inputfield
 * @param size The size of the inputfield
 * @param schema Commaseperated list of strings that specifie in n-depth.object.notation
 *				 the path to retrieve values from result. MUST match the datastructure 
 *				 returned from service, MINUS the rootnode.
 *		Example:
 *				 If data returned is JSON {"root":[{"key":"value"}]}, path must be set as "key"
 *				 The same is true if data is returned as XML, e.g. <root><key>value</key></root>
 *
-->
<#macro createAutoCompleteInputField fieldName description value width schema=[]>
  
  <#assign schemaString = "" />
  <#list schema as s>
    <#if schemaString = "">
      <#assign schemaString = "\"" + s + "\"" />
    <#else>
      <#assign schemaString = schemaString + ", \"" + s + "\"" />
    </#if>
  </#list>
 
  <div class="yui-skin-sam" style="margin-bottom:2em; width:${width}em !important;">
    <div id="${fieldName}.autoComplete">
      <input type="text" id="resource.${fieldName}" name="resource.${fieldName}" value="${value?html}" />
      <#if description != "">
        <span class="input-description">(${description})</span>
      </#if>
      <div id="${fieldName}.autoCompleteContainer"></div>
    </div>
    <script type="text/javascript">
    <!--
      var dataSource = new YAHOO.widget.DS_ScriptNode("/?vrtx=admin&action=autocomplete&field=${fieldName}", ["${fieldName}", ${schemaString}]);
      dataSource.scriptQueryParam = "${fieldName}"; 
      var autoComplete = new YAHOO.widget.AutoComplete("resource.${fieldName}", "${fieldName}.autoCompleteContainer", dataSource);
      autoComplete.delimChar = [","];
    //-->
    </script>
  </div>
</#macro>