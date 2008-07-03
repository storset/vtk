<#ftl strip_whitespace=true />
<#assign resource = resourceContext.currentResource>
<#if resource.URI == '/'>
  ${repositoryID?html}
<#else>
  ${resource.name?html}
</#if>
