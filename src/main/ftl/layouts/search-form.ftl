<#--
  - File: search-form.ftl
  - 
  - Description: Include a search form
  - 
  - Required model data:
  -   model
  -
  -->
<#import "../lib/vortikal.ftl" as vrtx />

<form method="get" action="${url.path?html}" class="vrtx-search-form">
   <fieldset>
    <input type="text" name="query" value="" class="vrtx-search-field" />
    <button type="submit">
      <span><@vrtx.msg code="decorating.searchFormComponent.search" default="Search"/></span>
    </button>
    <#list url.parameterNames as param>
      <#if url.getParameter(param)?exists>
    <input type="hidden" name="${param}" value="${url.getParameter(param)}" />
      </#if>
    </#list>
  </fieldset>
</form>