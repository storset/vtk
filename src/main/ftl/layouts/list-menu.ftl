2<#--
  - File: list-menu.ftl
  - 
  - Description: Simple list menu implementation
  - 
  - Required model data:
  -   menu
  -
  -->
<#assign cssClassMap = {
         "vertical":"vrtxVerticalMenu",
         "horizontal":"vrtxHorizontalMenu",
         "tabs":"vrtxTabsMenu" } />

<#if !menu?exists>
  <#stop "Unable to render model: required submodel
  'menu' missing">
</#if>
<ul class="${cssClassMap[menu.label]}">
  <#list menu.items as item>
    <#if item.url?exists>
      <#if item.active>
        <#if item.label?exists>
          <li class="active ${item.label}">
            <a href="${item.url?html}">${item.title?html}</a>
          </li>
        <#else>
          <li class="active">
            <a href="${item.url?html}">${item.title?html}</a>
          </li>
        </#if>
      <#else>
        <#if item.label?exists>
          <li class="${item.label}"><a href="${item.url?html}">${item.title?html}</a></li>
        <#else>
          <li><a href="${item.url?html}">${item.title?html}</a></li>
        </#if>
      </#if>
    </#if>
  </#list>
</ul>
