<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/person-search-form-util.ftl" as search />
<#import "/lib/view-tags.ftl" as tags />

<#assign heading = "Employee listing" />
<#if title??>
  <#assign heading = title />
</#if>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

  <head>
    <title>${heading?html}</title>
  </head>

  <body id="vrtx-unit-person-listing">

    <div id="vrtx-main-content">
      <div class="vrtx-frontpage-box white-box">

        <h1>
          ${heading?html}
          <#if headerPageNr?exists && (headerPageNr > 1)>
            - <@vrtx.localizeMessage code="unit-search.list-employees.page-nr" default="" args=["${headerPageNr}"] locale=locale />
          </#if>
        </h1>

        <#if missingAreacode??>
          <@vrtx.localizeMessage code="unit-search.no-areacode" default="" args=[] locale=locale />
        </#if>

        <#if unitMetadataNotFound??>
          <@vrtx.localizeMessage code="unit-search.no-unit-found" default="" args=["${areacode}"] locale=locale />
        </#if>

        <#if personSearchLimitExceeded?? || paging??>
          <@search.searchForm autocomplete='true' excludescripts='true' bigForm=true locale=locale />
        </#if>

        <#if employees??>
          [list employees here]
        </#if>

      </div><#-- class="vrtx-frontpage-box white-box" -->
    </div><#-- id="vrtx-main-content" -->

  </body>
</html>