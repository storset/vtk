<#ftl strip_whitespace=true>
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#if tabMenu2?exists>
  <@listMenu.listMenu menu=tabMenu2 displayForms=true/>
</#if>
