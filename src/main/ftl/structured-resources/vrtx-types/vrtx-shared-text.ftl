<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

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
    	       <#assign language >${resourceLocaleResolver.resolveLocale(null)}</#assign>
    	       <#if language == "no_NO">
    	       <#assign language = "no" />
    	       </#if>
    	       <#if sharedTextProps[inputFieldName][y]['description-' + language]?exists>
    	         <div class="${sharedTextProps[inputFieldName][y]['id']} shared-text-description">
    	           ${sharedTextProps[inputFieldName][y]['description-' + language]}
                 </div>
    	       <#else>
    	         <div class="${sharedTextProps[inputFieldName][y]['id']} shared-text-description">
    	           <@vrtx.msg code="shared-text.not-available" default="This shared text is not available in" /> <@vrtx.msg code="language.${language}" default="${language}" />.
    	         </div>
    	       </#if>
	       </#list>
	  </div>
    <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
  </div>
</div>
  <script type="text/javascript"><!--
            $(document).ready(function () {
                var h = $("#${inputFieldName}Descriptions").find(".shared-text-description");
                $(h).hide();
                if($("#${inputFieldName}").val() != ""){
                    var d =  $("#${inputFieldName}Descriptions").find("." + $("#${inputFieldName}").val().replace(/\./g, "\\."));
                    $(d).show();
                }
            }); 
  
            $("#${inputFieldName}").change(function(){
                var h = $("#${inputFieldName}Descriptions").find(".shared-text-description");
                $(h).hide();
                if($("#${inputFieldName}").val() != ""){
                    var d =  $("#${inputFieldName}Descriptions").find("." + $("#${inputFieldName}").val().replace(/\./g, "\\."));
                    $(d).show();
                }
            }); 
  // -->
  </script>
</#macro>
