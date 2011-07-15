<#ftl strip_whitespace=true />
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign resource = resourceContext.currentResource />

<#if resource?exists && resourceMenuLeft?exists && resourceMenuRight?exists>
  <@gen resource resourceMenuLeft resourceMenuRight />
<#elseif resource?exists && resourceMenuLeft?exists>
  <@gen resource resourceMenuLeft />
</#if>

<#macro gen resource resourceMenuLeft resourceMenuRight="">
  <div id="titleContainer">
    <div class="resource-title <@vrtx.iconResolver resource.resourceType resource.contentType /> ${resource.collection?string}">
      <#if (resourceMenuLeft.items?size > 0)>
        <h1>
      <#else>
        <h1 class="vrtx-comptact-header">
      </#if>
        <#if resource.URI == '/'>
          ${repositoryID?html}
        <#else>
          ${resource.name?html}
        </#if>
      </h1>
      <#if resourceMenuRight != "">
        <@listMenu.listMenu menu=resourceMenuRight displayForms=true prepend="" append=""/>
      </#if>
      <@listMenu.listMenu menu=resourceMenuLeft displayForms=true prepend="" append=""/>
    </div>
  </div>
</#macro>
