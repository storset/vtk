<#ftl strip_whitespace=true>
<#--
  - File: tag-cloud.ftl
  - 
  - Description: Simple rendering of a tag cloud as a list. Feel free to improve.
  - 
  - Required model data:
  -     tagElements - List<org.vortikal.web.view.decorating.components.TagCloudComponent.TagElement>
  - 
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />

<@createTagCloud />

<#macro createTagCloud title=false>
  <#if tagElements?exists && tagElements?size &gt; 0>
    <#if title>
	  <h3><@vrtx.msg code="decorating.tags" /></h3>
	</#if>
	<ul class="vrtx-tag-cloud">
	  <#list tagElements as element>
	    <li class="tag-magnitude-${element.magnitude}">
	      <a class="tag" href="${element.linkUrl?html}" rel="tag">${element.text?html}</a>
	    </li>
	  </#list>
	</ul>
  </#if>
</#macro>