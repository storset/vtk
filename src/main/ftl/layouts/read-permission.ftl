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
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign readPermission=""/>
<#if resourceContext.readPermission?exists && resourceContext.readPermission == "readProcessedAll">
  <#assign readPermission="readProcessedAll" />
</#if>

  <ul class="read-permission">
    <li class="${readPermission}">
      <@vrtx.msg code="readPermission${readPermission}" default="Allowed for all" />
    </li>
  </ul>