<#ftl strip_whitespace=true>
<#import "/lib/vtk.ftl" as vrtx />
<#assign lang><@vrtx.requestLanguage/></#assign>
<#assign url = helpURL />
<#if .vars["helpURL." + lang]?exists>
   <#assign url = .vars["helpURL." + lang] />
</#if>
<a href="${url?html}" target="_blank" class="help-link"><@vrtx.msg code="manage.help" default="Help in editing" /></a>
