<#--
  - File: subfolder-menu.ftl
  - 
  - Description: Sub folder menu implementation
  - 
  - Required model data:
  -   subFolderMenu
  -
  -->
<#if !subFolderMenu?exists>
  <#stop "Unable to render model: required submodel
  'subFolderMenu' missing">
</#if>

<#macro displayItem item>
  <#if item.menu?exists>
    <a href="${item.url?html}">${item.label}</a>
    <@displaySubMenu item.menu />
  <#else>
    <a href="${item.url?html}">${item.label}</a>
  </#if>
</#macro>

<#macro displayParentMenu menu currentCount>
  <ul class="resultset-${currentCount?html}">
    <#list menu.itemsSorted as item>
      <li> 
        <@displayItem item=item />
      </li>
    </#list>
  </ul>
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

<#if subFolderMenu.size &gt; 0>
  <#assign "counter" = 0>
  <#if subFolderMenu.resultSets?exists>
    <div class="vrtx-subfolder-menu">
      <#if subFolderMenu.title?exists>
        <div class="menu-title">${subFolderMenu.title?html}</div>
      </#if>
      <#list subFolderMenu.resultSets as resultSet>
        <#assign counter = counter+1>
        <@displayParentMenu menu=resultSet currentCount=counter />
      </#list>
    </div>
  </#if>
</#if>
