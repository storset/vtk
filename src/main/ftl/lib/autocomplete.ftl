
<#macro addAutoCompleteScripts srcBase>
  
  <script type='text/javascript' src='${srcBase}/jquery/jquery.autocomplete.js'></script>
  <link rel="stylesheet" type="text/css" href="${srcBase}/jquery/jquery.autocomplete.css" />

</#macro>

<#macro createAutoCompleteInputField appSrcBase fieldName value>

  <script type="text/javascript">
    $(document).ready(function() {
      $("#resource\\.${fieldName}").autocomplete('${appSrcBase}?vrtx=admin&action=autocomplete&field=${fieldName}', 
          { minChars:2, multiple:true });
    });
  </script>
  <input type="text" id="resource.${fieldName}" name="resource.${fieldName}" value="${value?html}" />
  
</#macro>