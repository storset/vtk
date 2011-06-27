<#--
  - File: publish.ftl
  - 
  - Description: Rendering publish info in header
  - 
  - Required model data:
  -   resourceContext
  -  
  - Optional model data:
  -   publish
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#-- Tmp JSON-check / TODO: assertions -->
<#if resourceContext.currentResource.contentType?exists 
  && resourceContext.currentResource.contentType == "application/json"> 
  
  <div class="publish resource-menu">
    <h3>${vrtx.getMsg("publishing.status")}</h3>
    <#assign published = resourceContext.currentResource.published />
    <#if published>
      <p><span class="published"><@vrtx.msg code="publishing.publish-date" /></span></p>
    <#else>
      <p><span class="unpublished"><@vrtx.msg code="publishing.unpublish-date" /></span></p>
    </#if>
  </div>
  
</#if>