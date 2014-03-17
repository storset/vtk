<#ftl strip_whitespace=true>
<#--
  - File: display-diff.ftl
  - 
  - Description: Displays (already computed) diff between two revisions
  - 
  - Required model data:
  -   revisionA - name of first revision
  -   revisionB - name of second revision
  -   content - the diff
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Diff between ${revisionA?html} and ${revisionB?html}</title>
    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" type="text/css" />
       </#list>
    </#if>
    <#if jsURLs??>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
  </head>
  
  <#assign resource = resourceContext.currentResource />
  
  <body id="vrtx-${resource.resourceType}">
    <#-- 
    <div id="vrtx-sticky-header">
      <div id="vrtx-sticky-header-inner">
        <span id="diff-header"><@vrtx.msg code="versions.table.title" default="Version" /> ${revisionBDetails.name?html}</span>
        <span id="diff-show-changes-info-nav">
          <span id="diff-show-changes-info">
          <span id="diff-info">
            <@vrtx.msg code="proptype.name.modifiedBy" default="Modified by" /> <span id="diff-info-modified-by">${revisionBDetails.principal.description?html}</span>, <@vrtx.date value=revisionBDetails.timestamp format="longlong" />
          </span>
          <#if original?exists>
            <form id="diff-show-changes-form" action="" method="get">
              <input id="diff-show-changes" name="original" type="checkbox" <#if !original>checked="checked"</#if> />
              <label for="diff-show-changes"><@vrtx.msg code="versions.diff.show-changes" default="Show changes" /></label>
            </form>
          </#if>
          </span>
          <span id="diff-nav">
            <#if revisionADetails?has_content>
              <a id="diff-nav-prev" href="?vrtx=diff&amp;revision=<#if revisionAPrevious?exists>${revisionAPrevious},</#if>${revisionADetails.name}"><@vrtx.msg code="previous" default="Previous" /></a>
            </#if>
            <#if revisionBNext?has_content>  
              <a id="diff-nav-next" href="?vrtx=diff&amp;revision=${revisionBDetails.name},${revisionBNext}"><@vrtx.msg code="next" default="Next" /></a>
            </#if>
          </span>
        </span>
      </div>
    </div> -->
      
    <#if content?has_content>
      ${content}
    </#if>
  </body>
</html>