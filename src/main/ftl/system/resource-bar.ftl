<#ftl strip_whitespace=true />
<#import "/lib/menu/list-menu.ftl" as listMenu />

<#assign resource = resourceContext.currentResource />

<@gen resource resourceMenuLeft resourceMenuRight />

<#macro gen resource resourceMenuLeft resourceMenuRight>
  <div id="titleContainer">
    <div class="resource-title ${resource.resourceType?html} ${resource.collection?string}">
      <h1>
        <#if resource.URI == '/'>
          ${repositoryID?html}
        <#else>
          ${resource.name?html}
        </#if>
      </h1>
      <@listMenu.listMenu menu=resourceMenuLeft displayForms=true prepend="" append=""/>
      <@listMenu.listMenu menu=resourceMenuRight displayForms=true prepend="" append=""/>
    </div>
  </div>
</#macro>
