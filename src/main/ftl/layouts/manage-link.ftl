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

<#if !manageLink?exists || !manageLink.url?exists>
  <#stop "Missing manageLink in model"/>
</#if>
<a class="vrtx-manage-url" href="${manageLink.url?html}">${manageLink.title}</a>
