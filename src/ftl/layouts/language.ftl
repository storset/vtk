<#--
  - File: language.ftl
  - 
  - Description: component for locale switching
  - 
  - Needed but not required model data:
  -  switchLocaleActions
  -
  -->
<#if switchLocaleActions?exists>
<#import "/lib/vortikal.ftl" as vrtx />

<div class="localeSelection">
<h3><@vrtx.msg code="localeSelection.selectLocale" default="Select locale"/></h3>
<ul>
<#list switchLocaleActions.localeServiceNames as locale>
  <li class="locale ${locale}">
    <#compress>
      <a href="${switchLocaleActions.localeServiceURLs[locale]?html}"><@vrtx.msg code="locales.${locale}" default="${locale}"/></a><#if locale_has_next> | </#if>
    </#compress>
  </li>
</#list>
</ul>
</div>

</#if>
