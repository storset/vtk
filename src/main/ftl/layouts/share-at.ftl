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

<div id="vrtx-share-component-wrapper">
  <a href="#share" id="share-link" class="vrtx-share-link" onclick="share();" name="share"><@vrtx.msg code="decorating.shareAtComponent.title" default="Share at" />...</a>
  <div id="vrtx-share-component">
    <div id="send-share">
      <div class="send-inner">
        <div class="share-top">
          <h3><@vrtx.msg code="decorating.shareAtComponent.title" default="Share at" />...</h3>
          <span><a href="#share" class="close-toolbox-send-share" onclick="share();"><@vrtx.msg code="decorating.shareAtComponent.close" default="Close" /></a></span>
        </div>
        <ul>
        <#list socialWebsites as socialWebsite>
          <li><a href="${socialWebsite.link}" class="${socialWebsite.name?lower_case}"><@vrtx.msg code="decorating.shareAtComponent.${socialWebsite.name?lower_case}" default="${socialWebsite.name}" /></a></li>
        </#list>
        </ul>
     </div>
    </div>
  </div>
</div>