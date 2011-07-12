<#--
  - File: list-menu.ftl
  - 
  - Description: Macro printing a menu structure using an unordered HTML list.
  -
  -->

<#-- listMenu macro:
 * The objects used by this macro are
 * org.vortikal.web.view.component.menu.ListMenu (holding the entire
 * menu), and org.vortikal.web.view.component.menu.MenuItem,
 * representing the individual menu items. Some menu items are
 * "active", meaning that they represent a URL to the current
 * service. In these cases a form, or some other rendering may take
 * place after the menu list itself is rendered.
 *
 * Custom freemarker templates may be supplied both for generating the
 * links in the menu bar, or for generating the form following the
 * menu list. 
 * 
 * TODO: Document the include-features
 * @param menu - a org.vortikal.web.view.component.menu.ListMenu object
 * @param displayForms - whether or not to attempt to display a form
 *        for the active menu item below the menu list (default 'false')
 * @param prepend - an optional string which is prepended to each link
 * @param between - an optional string which is inserted between links
 * @param append - an optional string which is appended to each link
 *
-->

<#import "/lib/vortikal.ftl" as vrtx/>

<#macro listMenu menu displayForms=false prepend="" between="" append="">

<#if menu.items?size &gt; 0 || menu.label == "resourceMenuRight">

<#-- Output the menu links: -->
<ul class="list-menu ${menu.label}">

  <#assign size = 0 />
  <#list menu.items as item> 
    <#if item.url?exists>
      <#assign size = size+1 />
    </#if>
  </#list>
  <#assign count = 1 />
  
  <#if menu.label == "resourceMenuRight">
    <#assign size = size+1 />
    <#if count == 1 && count == size>
      <li class="readPermission first last">
    <#elseif count == 1>
      <li class="readPermission first">
    </#if>
      <h3>${vrtx.getMsg("collectionListing.permissions")}</h3>
      <#if !resourceContext.currentResource.readRestricted >
        <p><span class="allowed-for-all">${vrtx.getMsg("collectionListing.permissions.readAll")}</span></p>
      <#else>
        <p><span class="restricted">${vrtx.getMsg("collectionListing.permissions.restricted")}</span></p>
      </#if>
    </li>
    <#assign count = count+1 />
  </#if>
  
  <#list menu.items as item> 
    <#if item.url?exists>
      <#if count == 1 && count == size>
        <li class="${item.label} first last">
      <#elseif count == 1>
        <li class="${item.label} first">     
      <#elseif count == size>
        <li class="${item.label} last">
      <#else>
        <li class="${item.label}">
      </#if>
        <#if item_index != 0 && item_index != menu.items?size>${between}</#if>
        <#attempt>
          <#include "/actions/list-menu.${item.label}.ftl" />
        <#recover>
          ${prepend}<a id="${item.label}" href="${item.url?html}">${item.title}</a>${append}
        </#recover>
      </li>
      <#assign count = count+1 />
    </#if>
  </#list>
</ul>

<#-- Output the form if it exists: -->
<#if displayForms && menu.activeItem?exists>
  <#attempt>
    <#include "/actions/list-menu.${menu.activeItem.label}.form.ftl" />
  <#recover>
    <#-- Do nothing -->
  </#recover>
</#if>

</#if>
</#macro>
