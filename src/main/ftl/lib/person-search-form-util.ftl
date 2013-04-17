
<#macro searchForm autocomplete='true' excludescripts='false' bigForm=false locale=springMacroRequestContext.getLocale()>

<#local formTitleMsgKey = 'person-search.label' />
<#if formTitleKey??>
<#local formTitleMsgKey = formTitleKey />
</#if>
<#local sc ='' />
<#if scope??>
  <#local sc = scope />
</#if>
<#local ac = '' />
<#if areacode??>
  <#local ac = areacode />
</#if>
<#local la = '' />
<#if lang??>
  <#local la = lang />
</#if>
<#local aff = '' />
<#if affiliation??>
  <#local aff = affiliation />
</#if>

<#if autocomplete?? && autocomplete = 'true'>
  <#if !excludescripts?? || excludescripts != 'true'>
    <#if jsURLs??>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
    <#if cssURLs??>
      <#list cssURLs as cssUrl>
        <link href="${cssUrl}" type="text/css" rel="stylesheet" />
      </#list>
    </#if>
  </#if>
  <script type="text/javascript">
    $(document).ready(function() {
      var autoCompleteParams = {minChars:3, selectFirst:false, width:233, max:50, delay:200, cacheLength:0};
      personerAutocomplete('person-search', 'person', autoCompleteParams, '${sc}', '${ac}', '${la}', '${aff}');
      gotoPersonerAutocompleteSuggestion('person-search');
    });
  </script>
</#if>

<#if bigForm>
  <form action="${url?html}" method="get" id="personer" class="vrtx-big-search">
<#else>
  <form action="${url?html}" method="get" id="personer">
</#if>
    <div>
      <fieldset>
        <label for="person-search"><@vrtx.localizeMessage code="${formTitleMsgKey}" default="" args=[] locale=locale /></label>
        <#if query??>
          <input name="person-query" id="person-search" class="ac_input" type="text" size="20" value="${query?html}" />
        <#else>
          <input name="person-query" id="person-search" class="ac_input" type="text" size="20" value="" />
        </#if>
        <button type="submit" class="searchsubmit">
          <span><@vrtx.localizeMessage code="person-search.submit" default="" args=[] locale=locale /></span>
        </button>
        <#list url.parameterNames as param>
          <#if url.getParameter(param)??>
            <input type="hidden" name="${param?html}" value="${url.getParameter(param)?html}" />
          </#if>
        </#list>
      </fieldset>
    </div>
  </form>

</#macro>