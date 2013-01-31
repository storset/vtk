<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#import "/pages/studies/view-course-group-listing.ftl" as courseGroup />
<#import "/pages/studies/view-course-description-listing.ftl" as courseDescription />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" type="text/css" />
      </#list>
    </#if>
    <#if printCssURLs?exists>
      <#list printCssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" media="print" type="text/css" />
      </#list>
    </#if>
    <#if jsURLs?exists>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
  
    <title>${collection.title?html}</title>
  
    <#if page?has_content>
      <#if "${page}" != "1"><meta name="robots" content="noindex, follow"/></#if>
    </#if>
  </head>
  <body id="vrtx-${collection.resourceType}">
    <h1>${collection.title?html}</h1>

    <#if filters?exists>
      <div id="vrtx-listing-filters" class="vrtx-listing-filters-${filters?size}-col">
        <#list filters?keys as filterKey>
          <#assign filter = filters[filterKey]>
          <div class="vrtx-listing-filters-section <#if (filterKey_index = (filters?size - 1))>vrtx-listing-filters-section-last</#if>" id="vrtx-listing-filters-section-${filterKey}">
            <#if useFilterTitle?? && useFilterTitle>
              <h2>${vrtx.getMsg("listing-filters.${filterKey}.title")}</h2>
            </#if>
            <ul>
            <#list filter?keys as parameterKey>
              <#assign url = filter[parameterKey].url>
              <#assign marked = filter[parameterKey].marked>
              <li id="vrtx-listing-filter-parameter-${filterKey}-${parameterKey}" class="vrtx-listing-filter-parameter<#if parameterKey = "all"> vrtx-listing-filter-parameter-all</#if><#if marked> vrtx-listing-filter-parameter-selected</#if>">
                <#if parameterKey = "all"> 
                  <a href="${url}">${vrtx.getMsg("listing-filters.filter.all")}</a>
                <#elseif filterKey = "semester"><#-- TODO: Hack to avoid year in i18n -->
                  <#if parameterKey?starts_with("v")>
                    <a href="${url}">${vrtx.getMsg("listing-filters.${filterKey}.filter.v")} 20${parameterKey?substring(1)}</a>
                  <#else>
                    <a href="${url}">${vrtx.getMsg("listing-filters.${filterKey}.filter.h")} 20${parameterKey?substring(1)}</a>
                  </#if>
                <#else>
                  <a href="${url}">${vrtx.getMsg("listing-filters.${filterKey}.filter.${parameterKey}")}</a>
                </#if>
              </li>
            </#list>
            </ul>
          </div>
        </#list>
      </div>
    </#if>

    <#if (result?exists && result?has_content)>
      <#if from?exists && to?exists && total?exists>
        <div id="vrtx-listing-filter-hits">
          <@vrtx.msg code="listing-filters.${collection.resourceType}.from-to-total" args=[from, to, total] default="Showing " + from + "&ndash;" +  to + " of " + total + " resources" />
        </div>
      </#if>
      <#if collection.resourceType = 'course-group-listing'>
        <@courseGroup.displayResult result />
      <#elseif collection.resourceType = 'course-description-listing'>
        <@courseDescription.displayResult result />
      <#else>
        <ul id="vrtx-listing-filter-results">
          <#list result as res>
            <#assign title = vrtx.propValue(res, 'title') />
            <#assign uri = vrtx.getUri(res) />
            <li><a href="${uri}">${title}</a></li>
          </#list>
        </ul>
      </#if>
    </#if>

    <#if pageThroughUrls?exists && (pageThroughUrls?size > 1) >
      <div class="vrtx-paging-feed-wrapper">
        <@viewutils.displayPageThroughUrls pageThroughUrls page />
      </div>
    </#if>
    
    <#if conf?exists && collection.resourceType = 'course-group-listing'>
      <div id="vrtx-listing-filter-status"><@courseGroup.displayStatusLink conf  /></div>
    </#if>

  </body>
</html>