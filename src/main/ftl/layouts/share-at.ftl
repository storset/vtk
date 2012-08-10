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

<!-- begin share js -->
<script type="text/javascript" src="${url?html}"></script>
<!-- end share js -->
  
<#assign title = vrtx.getMsg("decorating.shareAtComponent.title") + "..." />
  
<@viewutils.displayShareSubNestedList title>
  <#list socialWebsites as socialWebsite>
    <li>
      <a href="${socialWebsite.url}" target="_blank" class="${socialWebsite.name?lower_case}">
        <@vrtx.msg code="decorating.shareAtComponent.${socialWebsite.name?lower_case}" default="${socialWebsite.name}" />
      </a>
    </li>
  </#list>   
</@viewutils.displayShareSubNestedList>