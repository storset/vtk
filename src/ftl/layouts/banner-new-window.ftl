<#--
  - File: banner-browse.ftl
  - 
  - Description: Simple browse banner.
  - 
  - Todo: Need to make a generic banner component. 
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<div class="banner">
  <div style="margin-top: -3px;">
     <span class="close" style="padding-right: 15px;"><a href=""
     onclick="window.close()"><@vrtx.msg code="default.window.close"
     default="close window"/></a></span>
  </div>
</div>

