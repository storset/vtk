<#--
  - File: read-permission.ftl
  - 
  - Description: Rendering read permission info in header
  - 
  - Required model data:
  -   resourceContext
  -  
  - Optional model data:
  -   readPermission
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#assign readPermission=""/>
<#if resourceContext.currentResource?exists && resourceContext.currentResource.isReadRestricted()>
  <#assign readPermission="readPermissionRestricted" />
</#if>

  <ul class="read-permission">
    <li class="${readPermission}">
      <@vrtx.msg code="readPermission${readPermission}" default="Allowed for all" />
    </li>
  </ul>