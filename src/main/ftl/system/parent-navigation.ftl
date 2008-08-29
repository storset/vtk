<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#if tabMenu1?exists>
    <ul class="listMenu tabMenu1">
    <#if (tabMenu1.url)?exists>
      <li class="navigateToParentService">
        <a href="${tabMenu1.url?html}">
          <@vrtx.msg code="collectionListing.navigateToParent" default="Up"/>
        </a>
      </li>
    </#if>
   </ul>
</#if>
