<#--
  - File: manage-link.ftl
  - 
  - Description: inserts a link to the manage service inside a <div>
  -  
  - Required model data:
  -  
  - Optional model data:
  -   manageLink
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#if manageLink?exists && manageLink.url?exists>
  <div class="manageLink">
    <a href="${manageLink.url?html}"><@vrtx.msg code="manage.folder" default="Manage" /></a>
  </div>
</#if>
