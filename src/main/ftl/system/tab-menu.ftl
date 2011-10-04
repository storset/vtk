<#ftl strip_whitespace=true>
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#if tabMenuRight?exists>
  <@listMenu.listMenu menu=tabMenuRight displayForms=true/>
</#if>
