<#ftl strip_whitespace=true>
<#--
  - File: read-permission.ftl
  - 
  - Description: Rendering read permission info in header
  - 
  - Required model data:
  -   resourceContext
  -  
  - Optional model data:
  -   readPermissions
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<div class="read-permissions resource-menu">
  <h3>${vrtx.getMsg("collectionListing.permissions")}</h3>
  <#if !resourceContext.currentResource.readRestricted >
    <p><span class="allowed-for-all">${vrtx.getMsg("collectionListing.permissions.readAll")}</span></p>
  <#else>
    <p><span class="restricted">${vrtx.getMsg("collectionListing.permissions.restricted")}</span></p>
  </#if>
</div>
