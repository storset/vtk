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
<#macro listMenu menu displayForms=false prepend="" between="" append="">

<#if menu.items?size &gt; 0>

<#-- Output the menu links: -->
<ul class="listMenu ${menu.label}">
  <#list menu.items as item> 
    <#if item.url?exists>
      <li class="${item.label}">
        <#if item_index != 0 && item_index != menu.items?size>${between}</#if>
        <#attempt>
          <#include "/actions/list-menu.${item.label}.ftl" />
        <#recover>
          ${prepend}<a href="${item.url?html}">${item.title}</a>${append}
        </#recover>
      </li>
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
