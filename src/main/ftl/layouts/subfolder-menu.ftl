<#--
  - File: subfolder-menu.ftl
  - 
  - Description: Sub folder menu implementation
  - 
  - Required model data:
  -   subFolderMenu
  -
  -->
  
 <#-- RECURSIVE MENU BUILD --> 
 <#if subFolderMenu?exists>
    <#if subFolderMenu.size &gt; 0>
      <#assign "counter" = 0>
      <#assign "counter2" = 0>
         
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
               <@displayParentMenu menu=resultSet currentCount=counter newDiv=true />
            <#else>
               <@displayParentMenu menu=resultSet currentCount=counter newDiv=false />
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

<#macro displayParentMenu menu currentCount newDiv>
  <#if newDiv>
       <#if currentCount != 1>
          </div>
       </#if>  
          <div class="vrtx-group-${subFolderMenu.groupResultSetsBy?html}">
  </#if>
  <ul class="resultset-${currentCount?html}">
    <#list menu.itemsSorted as item>
      <li> 
        <@displayItem item=item />
      </li>
    </#list>
  </ul>
  
  <#if currentCount == subFolderMenu.resultSets?size>
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
  </ul>
</#macro>