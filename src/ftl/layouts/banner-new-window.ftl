<#--
  - File: banner-browse.ftl
  - 
  - Description: Simple browse banner.
  - 
  - Todo: Need to make a generic banner component. 
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<#if resourceContext?exists>
  <div class="banner">
    <div class="text">
      <span class="hostname">${resourceContext.repositoryId}</span>
      <span><@vrtx.msg code="browse.bannerText" default="Create link to a document" /></span>      
    </div>
    <div class="menu">
     <span class="close" style="padding-right: 15px;">
     <a href="" onclick="window.close()"><@vrtx.msg code="default.window.close" default="close window"/></a></span>
    </div>
  </div>
</#if>

