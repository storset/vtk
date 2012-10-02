<#ftl strip_whitespace=true>
<#--
  - File: share-at.ftl
  - 
  - Description: Share document on social websites
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<!-- begin view dropdown js -->
<script type="text/javascript" src="${jsUrl?html}"></script>
<!-- end view dropdown js -->
  
<#assign title = vrtx.getMsg("decorating.shareAtComponent.title") + "..." />
  
 <#-- TODO: use some sort of alternative url from different mapping instead? -->
<#if .vars['use-facebook-api']?exists && .vars['use-facebook-api']>
  <#assign skip = "Facebook" />
<#else>>
  <#assign skip = "FacebookAPI" />
</#if> 
  
<@viewutils.displayDropdown "share" title>
  <#list socialWebsites as socialWebsite>
    <#if skip != socialWebsite.name>
      <li>
        <a href="${socialWebsite.url}" target="_blank" class="${socialWebsite.name?lower_case}">
          <@vrtx.msg code="decorating.shareAtComponent.${socialWebsite.name?lower_case}" default="${socialWebsite.name}" />
        </a>
      </li>
    </#if>
  </#list>   
</@viewutils.displayDropdown>