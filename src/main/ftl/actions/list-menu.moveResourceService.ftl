<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if resourceContext.parentURI = "/">
  <#if resourceContext.currentResource.collection>
    <#assign actionURL = vrtx.linkConstructor('../', 'manageService') />
  <#else>
    <#assign actionURL = vrtx.linkConstructor('./', 'manageService') />  
  </#if>
<#else>
  <#assign actionURL = vrtx.linkConstructor(resourceContext.parentURI, 'manageService') />
</#if>

<form method="post" action="${actionURL?html}" name="moveResourceServiceForm">
  <@vrtx.csrfPreventionToken url=actionURL />
  <input type="hidden" name="action" value="move-resources"  />
  <input type="hidden" name="${resourceContext.currentURI}" />
  <input type="submit" value="${item.title}" />
</form>
     
<#recover>
${.error}
</#recover>