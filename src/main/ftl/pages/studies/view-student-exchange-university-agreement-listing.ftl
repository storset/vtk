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
          <@vrtx.msg code="${collection.resourceType}.from-to-total" args=[from, to, total] default="Showing " + from + "-" +  to + " of " + total + " resources" />
        </div>
      </#if>

      <table id="vrtx-${collection.resourceType}-results" class="sortable">
        <thead>
          <tr>
            <th class="sortable"><@vrtx.msg code="${collection.resourceType}.owner-of-agreement" default="Owner of agreement" /></th>
            <th class="sortable"><@vrtx.msg code="${collection.resourceType}.level" default="Level" /></th>
            <th class="sortable"><@vrtx.msg code="${collection.resourceType}.type-of-agreement" default="Type" /></th>
          </tr>
        </thead>
        <tbody>
          <#list result as res>
            <#assign owner = vrtx.propValue(res, 'owner-of-agreement') />
            <#assign uri = vrtx.getUri(res) />
            <#assign type = vrtx.propValue(res, 'type-of-agreement') />
            <#assign level = checkAndAddLevels(res, ['bachelor', 'master', 'phd', 'profesjonsstudium', 'specialist']) />
            <tr>
              <td><a href="${uri?html}">${owner?html}</a></td>
              <td>${level?html}</td>
              <td><@vrtx.msg code="${collection.resourceType}.type-of-agreement.${type?html}" default="${type?html}" /></td>
            </tr>
          </#list>
        </tbody>
      </table>

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

<#function checkAndAddLevels res levels>
  <#assign acc = "" />
  <#list levels as level>
    <#if vrtx.propValue(res, level)?eval>
      <#assign localizedLevel>
        <@vrtx.msg code="${collection.resourceType}.level.${level?html}" default="${level}" />
      </#assign>
      <#if (acc != "")>
        <#assign acc = acc + ", " + localizedLevel />
      <#else> 
        <#assign acc = localizedLevel />
      </#if>
    </#if>
  </#list>
  <#assign localizedAnd>
    <@vrtx.msg code="${collection.resourceType}.level.and" default="and" />
  </#assign>
  <#assign acc = acc?replace(',([^,]+)$', ' ${localizedAnd?html} $1', 'r') />
  <#return acc />
</#function>