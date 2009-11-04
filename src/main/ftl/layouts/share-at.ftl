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

<div id="vrtx-share-component">
  <a href="#vrtx-share-link" id="vrtx-share-link" class="vrtx-share-link" onclick="share();" name="vrtx-share-link">
  <@vrtx.msg code="decorating.shareAtComponent.title" default="Share at" />...</a>
    <div id="vrtx-send-share">
      <div class="vrtx-send-inner">
        <div class="vrtx-share-top">
          <div class="vrtx-share-title"><@vrtx.msg code="decorating.shareAtComponent.title" default="Share at" />...</div>
          <span><a href="#share" class="vrtx-close-toolbox-send-share" onclick="share();">
          <@vrtx.msg code="decorating.shareAtComponent.close" default="Close" /></a></span>
        </div>
        <ul>
        <#list socialWebsites as socialWebsite>
          <li><a href="${socialWebsite.url}" target="_blank" class="${socialWebsite.name?lower_case}">
          <@vrtx.msg code="decorating.shareAtComponent.${socialWebsite.name?lower_case}" default="${socialWebsite.name}" /></a></li>
        </#list>
        </ul>
     </div>
    </div>
</div>