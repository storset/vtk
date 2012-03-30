<#ftl strip_whitespace=true>
<#--
  - File: manage-link.ftl
  - 
  - Description: inserts a link (href) to the manage service
  -  
  - Required model data:
  -   manageLink
  -  
  - Optional model data:
  -   config
  -   resourceContext
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#if !manageLink?exists || !manageLink.url?exists>
  <#stop "Missing 'manageLink' entry in model"/>
</#if>

<#if resourceContext?exists
     && .vars['display-only-if-auth']?exists
     && .vars['display-only-if-auth'] = 'true'
     && !resourceContext.principal?exists>

  <#-- Display nothing -->
<#else>
  <a id="vrtx-manage-url" class="vrtx-manage-url" href="${manageLink.url?html}">${manageLink.title}</a>
</#if>