<#ftl strip_whitespace=true>
<#--
  - File: select-language.ftl
  - 
  - Description: component for locale switching
  - 
  - Needed but not required model data:
  -  switchLocaleActions
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#if switchLocaleActions?exists>
  <#compress>
    <div id="locale-selection">
      <span id="locale-selection-header"><@vrtx.msg code="localeSelection.selectLocale" default="Language settings"/>:</span>
      <ul>
        <#list switchLocaleActions.localeServiceNames as locale>
          <#assign active = switchLocaleActions.localeServiceActive[locale]?html />
          <li class="locale ${locale} ${active}">
            <#if active = "active">
              <span><@vrtx.msg code="locales.${locale}" default="${locale}"/></span>
            <#else>
              <a href="${switchLocaleActions.localeServiceURLs[locale]?html}"><@vrtx.msg code="locales.${locale}" default="${locale}"/></a>
            </#if>
          </li>
        </#list>
      </ul>
    </div>
  </#compress>
</#if>
