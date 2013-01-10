<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>

  </head>
  <body id="vrtx-${resource.resourceType}">

    <#if filters?exists>
      <div id="vrtx-listing-filters" class="vrtx-listing-filters-${filters?size}-col">
        <#list filters?keys as filterKey>
          <#assign filter = filters[filterKey]>
          <div class="vrtx-listing-filters-section <#if (filterKey_index = (filters?size - 1))>vrtx-listing-filters-section-last</#if>" id="vrtx-listing-filters-section-${filterKey}">
            <h2>${filterKey}</h2>
            <ul>
            <#list filter?keys as parameterKey>
              <#assign url = filter[parameterKey].url>
              <#assign marked = filter[parameterKey].marked>
              <li id="vrtx-listing-filter-parameter-${filterKey}-${parameterKey}" class="vrtx-listing-filter-parameter<#if parameterKey = "all"> vrtx-listing-filter-parameter-all</#if><#if marked> vrtx-listing-filter-parameter-selected</#if>"><a href="${url}">${parameterKey}</a></li>
            </#list>
            </ul>
          </div>
        </#list>
      </div>
    </#if>

    <#if (result?exists && result?has_content)>
      <div>
        <#list result as res>
          <#assign title = vrtx.propValue(res, 'title') />
          <#assign uri = vrtx.getUri(res) />
          <p>
            <a href="${uri}">${title}</a>
          </p>
        </#list>
      </div>
    </#if>

    <#if discontinuedUrl?exists>
      <div>
        <a href="${discontinuedUrl}">Discontinued</a>
      </div>
    <#elseif currentUrl?exists>
      <div>
        <a href="${currentUrl}">Current</a>
      </div>
    </#if>

  </body>
</html>