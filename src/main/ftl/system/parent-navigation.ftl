<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#if tabMenu1?exists>
  <#if (tabMenu1.url)?exists>
    <ul class="list-menu" id="tabMenu1">
      <li class="navigateToParentService">
        <a id="navigateToParent" href="${tabMenu1.url?html}">
          <@vrtx.msg code="collectionListing.navigateToParent" default="Up"/>
        </a>
      </li>
    </ul>
  </#if>
</#if>
