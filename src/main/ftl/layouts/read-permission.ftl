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

  <ul class="read-permission">
    <li class="${resourceContext.readPermission}">
      <@vrtx.msg code="readPermission${resourceContext.readPermission}" default="Allowed for all" />
    </li>
  </ul>