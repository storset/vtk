<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="" inputFieldSize=20 valuemap="" dropdown=false defaultValue="">
<div class="vrtx-string ${classes}">
  <label for="${inputFieldName}">${title}</label>
  <div class="inputfield">
      <#if value=="" >
        <#local value=defaultValue />
      </#if> 
      <select name="${inputFieldName}" id="${inputFieldName}">
          <option value="" <#if value=="">selected="selected"</#if>>Ingen fellestekst</option>
          <#list sharedTextProps[inputFieldName]?keys as y >
            <option value="${sharedTextProps[inputFieldName][y]['id']?html}" <#if value==sharedTextProps[inputFieldName][y]['id']>selected="selected"</#if>>${sharedTextProps[inputFieldName][y]['title']?html}</option>
          </#list>     
	  </select>
	  <div id="${inputFieldName}Descriptions">
	       <#list sharedTextProps[inputFieldName]?keys as y >
    	       <div class="${sharedTextProps[inputFieldName][y]['id']} descriptionxxx">
    	              <#assign language >${resourceLocaleResolver.resolveLocale(null)}</#assign>
    	              <#if language == "no_NO">
    	              <#assign language = "no" />
    	              </#if>
    	              <#if sharedTextProps[inputFieldName][y]['description-' + language]?exists>
    	                 ${sharedTextProps[inputFieldName][y]['description-' + language]}
    	              </#if>
    	       </div>
	       </#list>
	  </div>
    <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
  </div>
</div>
  <script type="text/javascript"> 
  <!--
            $(document).ready(function () {
                var h = $("#${inputFieldName}Descriptions").find(".descriptionxxx");
                $(h).hide();
                if($("#${inputFieldName}").val() != ""){
                    var d =  $("#${inputFieldName}Descriptions").find("." + $("#${inputFieldName}").val().replace(/\./g, "\\."));
                    $(d).show();
                }
            }); 
  
            $("#${inputFieldName}").change(function(){
                var h = $("#${inputFieldName}Descriptions").find(".descriptionxxx");
                $(h).hide();
                if($("#${inputFieldName}").val() != ""){
                    var d =  $("#${inputFieldName}Descriptions").find("." + $("#${inputFieldName}").val().replace(/\./g, "\\."));
                    $(d).show();
                }
            }); 
  -->
  </script>
</#macro>
