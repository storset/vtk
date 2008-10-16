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
<script type="text/javascript" src="${srcBase}/build/datasource/datasource-min.js"></script> 
<script type="text/javascript" src="${srcBase}/build/autocomplete/autocomplete-min.js"></script>

</#macro>

<#--
 * createAutoCompleteInputField
 *
 * Creates an inputfield with added functionality for autocomplete.
 * Example: <@createAutoCompleteInputField fieldName="myInput" value="myInputValue" schema="['dataStructure']"
 *
 * @param appSrcBase The appResourceURL that the service is matched to
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
<#macro createAutoCompleteInputField appSrcBase fieldName description value width schema=[]>
  
  <#local schemaString = "" />
  <#list schema as s>
    <#if schemaString = "">
      <#local schemaString = "\"" + s + "\"" />
    <#else>
      <#local schemaString = schemaString + ", \"" + s + "\"" />
    </#if>
  </#list>
 
  <div style="padding-bottom:1.6em;">
    <#-- Must add "display: block !important;" to the style, otherwise... @see editor.css->div.yui-skin-sam -->
    <div class="yui-skin-sam" style="display: block !important; float: left; padding-bottom:.8em; width:${width}em !important;">
      <div id="${fieldName}.autoComplete">
        <input type="text" id="resource.${fieldName}" name="resource.${fieldName}" value="${value?html}" />
        <div id="${fieldName}.autoCompleteContainer"></div>
      </div>
      <script type="text/javascript">
      <!--
        var dataSource = new YAHOO.util.ScriptNodeDataSource("${appSrcBase}/?vrtx=admin&action=autocomplete&field=${fieldName}");
        dataSource._aDeprecatedSchema = ["${fieldName}", ${schemaString}];
        dataSource.scriptQueryParam = "${fieldName}"; 
        var autoComplete = new YAHOO.widget.AutoComplete("resource.${fieldName}", "${fieldName}.autoCompleteContainer", dataSource);
        autoComplete.delimChar = [","];
        autoComplete.maxResultsDisplayed = 5; 
      //-->
      </script>
    </div>
    <#if description != "">
      <div style="float: left; padding-left: .7em; margin-top:.3em;">
        <span class="input-description">(${description})</span>
      </div>
    </#if>
  </div>
</#macro>