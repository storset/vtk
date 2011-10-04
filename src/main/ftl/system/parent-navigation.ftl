<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#if tabMenuLeft?exists>
  <#if (tabMenuLeft.url)?exists>
    <ul class="list-menu" id="tabMenuLeft">
      <li class="navigateToParentService">
        <a id="navigateToParentService" href="${tabMenuLeft.url?html}">
          <@vrtx.msg code="collectionListing.navigateToParent" default="Up"/>
        </a>
      </li>
    </ul>
  </#if>
</#if>
