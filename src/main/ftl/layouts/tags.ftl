<#--
  - File: tags.ftl
  - 
  - Description: Simple rendering of a tags as a list. Feel free to improve.
  - 
  - Required model data:
  -     tagElements - List<org.vortikal.web.view.decorating.components.TagCloudComponent.TagElement>
  - 
  -->

<#if tagElements?exists>
<div id="vrtx-tags">
	<#assign i=1 >
    <ul class="vrtx-tags-${i?html}">
     <#assign counter=0 >
     <#assign i = i +1>
     <#list tagElements as element>
     
     <#if counter == numberOfTagsOnEachRow>
	     </ul>
	     <ul  class="vrtx-tags-${i?html}">
	     <#if completeRows == 0 >
	     	<#assign numberOfTagsOnEachRow = numberOfTagsOnEachRow - 1>
	     </#if>
	     <#assign counter=0 >
	     <#assign i = i +1>
	     <#if completeRows &gt; -1 >
	     	<#assign completeRows = completeRows - 1>
	     </#if>
     </#if>
     
       <li class="tag-element">
         <a class="tag" href="${element.linkUrl?html}" rel="tag">${element.text?html}</a>
         <#if showOccurence >
         	(${element.occurences?html})
         </#if>
       </li>
      
   
     <#assign counter=counter + 1 >
     </#list>
    </ul>
</div>
</#if>
