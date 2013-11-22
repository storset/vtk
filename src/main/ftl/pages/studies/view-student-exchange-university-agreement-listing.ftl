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

    <#if (result?exists && result?has_content)>
      <#if from?exists && to?exists && total?exists>
        <div id="vrtx-${collection.resourceType}-hits">
          <@vrtx.msg code="vrtx-${collection.resourceType}.from-to-total" args=[from, to, total] default="Showing " + from + "-" +  to + " of " + total + " resources" />
        </div>
      </#if>

      <div id="vrtx-${collection.resourceType}-results">
        <ul>
          <#list result as res>
            <#assign title = vrtx.propValue(res, 'title') />
            <#assign uri = vrtx.getUri(res) />
            <li><a href="${uri}">${title}</a></li>
          </#list>
        </ul>
      </div>

      <#if pageThroughUrls?exists && (pageThroughUrls?size > 1) >
        <div class="vrtx-paging-feed-wrapper">
          <@viewutils.displayPageThroughUrls pageThroughUrls page />
        </div>
      </#if>
    <#else>
      <div id="vrtx-${collection.resourceType}-no-results">
        <@vrtx.msg code="${collection.resourceType}.no-result" default="No result" />
      </div>
    </#if>

  </body>
</html>
