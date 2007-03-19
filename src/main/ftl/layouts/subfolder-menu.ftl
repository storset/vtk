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
<#if subFolderMenu.size &gt; 0>
  <#if subFolderMenu.resultSets?exists>
    <div class="vrtxSubFolderMenu">
      <#if subFolderMenu.title?exists>
        <div class="menuTitle">${subFolderMenu.title?html}</div>
      </#if>
    </div>
    <#list subFolderMenu.resultSets as resultSet>
      <#if resultSet.items?exists && resultSet.items?size &gt; 0>
        <ul class="vrtxSubFolderMenu">
          <#list resultSet.items as item>
            <li><a href="${item.url?html}">${item.title?html}</a></li>
          </#list>
        </ul>
      </#if>
    </#list>
  </#if>
</#if>
