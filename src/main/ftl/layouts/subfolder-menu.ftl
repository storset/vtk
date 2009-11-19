<#--
  - File: subfolder-menu.ftl
  - 
  - Description: Sub folder menu implementation
  - 
  - Required model data:
  -   subFolderMenu
  -
  -->
  <#import "../lib/vortikal.ftl" as vrtx />
 <#-- RECURSIVE MENU BUILD --> 
 <#if subFolderMenu?exists>
    <#if subFolderMenu.size &gt; 0>
      <#assign "counter" = 0>
      <#assign "counter2" = 0>
      <#assign "c" = 0>
         
      <#if subFolderMenu.resultSets?exists>
        <div class="vrtx-subfolder-menu">
          <#if subFolderMenu.title?exists>
            <div class="menu-title">${subFolderMenu.title?html}</div>
          </#if>
          <#list subFolderMenu.resultSets as resultSet>
            <#assign counter = counter+1>
            <#assign counter2 = counter2+1>
            <#if subFolderMenu.groupResultSetsBy?exists && (subFolderMenu.groupResultSetsBy?number = counter2 || counter = 1)>
               <#assign "counter2" = 0>
               <#assign c = c+1>
               <@displayParentMenu menu=resultSet currentCount=counter groupCount=c newDiv=true />
            <#else>
               <@displayParentMenu menu=resultSet currentCount=counter groupCount=c newDiv=false />
            </#if>
          </#list>
        </div>
      </#if>
    </#if>
</#if>

<#-- MACROS: -->
<#macro displayItem item>
  <#if item.menu?exists>
    <a href="${item.url?html}">${item.label}</a>
    <@displaySubMenu item.menu />
  <#else>
    <a href="${item.url?html}">${item.label}</a>
  </#if>
</#macro>

<#macro displayParentMenu menu currentCount groupCount newDiv>
  <#if newDiv>
       <#if currentCount != 1>
          </div>
       </#if>  
          <div class="vrtx-group-${groupCount?html}">
  </#if>
  <ul class="resultset-${currentCount?html}">
    <#list menu.itemsSorted as item>
      <li> 
        <@displayItem item=item />
      </li>
    </#list>
  </ul>
  
  <#if subFolderMenu.groupResultSetsBy?exists && subFolderMenu.groupResultSetsBy?number &gt; 0 && currentCount == subFolderMenu.resultSets?size>
     </div>
  </#if>
</#macro>

<#macro displaySubMenu menu>
  <ul>
    <#list menu.itemsSorted as item>
      <li>   
        <@displayItem item=item />
      </li>
    </#list>
	<#if (menu.totalNumberOfItems > menu.maxNumberOfItems)>
	    <li class="vrtx-more">   
			<a href="${menu.moreUrl?html}"><@vrtx.msg code="subfolder.morelinkTitle" /></a>
		</li>
	</#if>
  </ul>
</#macro>