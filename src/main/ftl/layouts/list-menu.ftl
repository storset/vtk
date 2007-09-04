<#--
  - File: list-menu.ftl
  - 
  - Description: Simple list menu implementation
  - 
  - Required model data:
  -   menu
  -
  -->
<#assign cssClassMap = {
         "none":"vrtx-uri-menu",
         "vertical":"vrtx-vertical-menu",
         "horizontal":"vrtx-horizontal-menu",
         "tabs":"vrtx-tab-menu" } />

<#if !menu?exists>
  <#stop "Unable to render model: required submodel
  'menu' missing">
</#if>


<#-- RECURSIVE MENU BUILD -->
<ul class="${cssClassMap[menu.label]}">
  <@listMenu menu=menu />
</ul>


<#-- MACROS: -->
<#macro listMenu menu>
  <#list menu.items as item>
    <#if item.url?exists>
      <@listItem item=item />
    </#if>
  </#list>
</#macro>


<#macro listItem item>
  <#if item.active>
    <#if item.label?exists>
      <li class="vrtx-active-item ${item.label}">
        <@displayItem item=item/>
      </li>
    <#else>
      <li class="vrtx-active-item">
        <@displayItem item=item/>
      </li>
    </#if>
  <#else>
    <#if item.label?exists>
      <li class="${item.label}">
      	<@displayItem item=item/>
      </li>
    <#else>
      <li>
      	<@displayItem item=item/>
      </li>
    </#if>
  </#if>
</#macro> 


<#macro displayItem item>
  <a href="${item.url?html}">${item.title?html}</a>
  <#if item.menu?exists>
    <#if item.menu.label?exists>
      <ul class="${item.menu.label}">
        <@listMenu menu=item.menu />
      </ul>
    <#else>
      <ul>
        <@listMenu menu=item.menu />
      </ul>    
    </#if>
  </#if>
</#macro>
