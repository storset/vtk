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

<form method="get" action="${uri}" class="vrtx-search-form">
   <fieldset>
    <input type="hidden" name="vrtx" value="search"/>
    <input type="text" name="query" value="" class="vrtx-search-field" />
    <button type="submit"><span><@vrtx.msg code="decorating.searchFormComponent.search" default="Search"/></span></button>
  </fieldset>
</form>