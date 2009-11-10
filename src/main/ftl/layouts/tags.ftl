<#--
  - File: tags.ftl
  - 
  - Description: Simple rendering of a tags as a list. Feel free to improve.
  - 
  - Required model data:
  -     tagElements - List<org.vortikal.web.view.decorating.components.TagCloudComponent.TagElement>
  - 
  -->

<#if tagElements?exists && tagElements?size &gt; 0>

<#assign resourceTypeParam = "" />
<#if resourceTypes?exists && (resourceTypes?size > 0)>
  <#list resourceTypes as resourceType>
    <#if !resourceTypeParam?has_content>
      <#assign resourceTypeParam = "resource-type=" + resourceType.name />
    <#else>
      <#assign resourceTypeParam = resourceTypeParam + "&resource-type=" + resourceType.name />
    </#if>
  </#list>
</#if>

<div id="vrtx-tags">
	<#assign i=1 >
    <ul class="vrtx-tags-${i?html}">
     <#assign counter=0 >
     <#assign i = i +1>
     <#list tagElements as element>     
	     <#if counter == numberOfTagsInEachColumn>
		     </ul>
		     <ul  class="vrtx-tags-${i?html}">
		     <#if completeColumn == 0 >
		     	<#assign numberOfTagsInEachColumn = numberOfTagsInEachColumn - 1>
		     </#if>
		     <#assign counter=0 >
		     <#assign i = i +1>
		     <#if completeColumn &gt; -1 >
		     	<#assign completeColumn = completeColumn - 1>
		     </#if>
	     </#if>
	       <li class="vrtx-tags-element">
	         <#assign linkUrl = element.linkUrl />
	         <#if resourceTypeParam?has_content>
	           <#assign linkUrl = linkUrl + "&" + resourceTypeParam />
	         </#if>
	         <a class="tags" href="${linkUrl?html}" rel="tags">${element.text?html}</a>
	         <#if showOccurence >
	         	(${element.occurences?html})
	         </#if>
	       </li>
	     <#assign counter=counter + 1 >
     </#list>
    </ul>
</div>
</#if>
