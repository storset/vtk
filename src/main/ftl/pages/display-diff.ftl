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
    <script type="text/javascript"><!--
      $(document).ready(function() {
        var diffStickyBar = new VrtxStickyBar({
          wrapperId: "#vrtx-sticky-header",
          stickyClass: "vrtx-sticky-header-shadow",
          contentsId: "body",
          alwaysFixed: true
        });
        $("#vrtx-sticky-header").on("click", "#diff-show-changes", function() {
          if(!this.checked) {
            location.href = location.href + "&" + this.name;
          } else {
            location.href = location.href.replace("&" + this.name, "");
          }
        });
      });
    // -->
    </script>
  </head>
  
  <#assign resource = resourceContext.currentResource />
  
  <#if revisionBDetails?exists>
    <body id="vrtx-${resource.resourceType}">
      <div id="vrtx-sticky-header">
        <div id="vrtx-sticky-header-inner">
           <span id="diff-header"><@vrtx.msg code="versions.table.title" default="Version" /> ${revisionBDetails.name?html}</span>
           <span id="diff-show-changes-info-nav">
             <span id="diff-show-changes-info">
               <span id="diff-info">
                 <@vrtx.msg code="proptype.name.modifiedBy" default="Modified by" /> <span id="diff-info-modified-by">${revisionBDetails.principal.description?html}</span>, <@vrtx.date value=revisionBDetails.timestamp format="longlong" />
               </span>
               <form id="diff-show-changes-form" action="" method="get">
                 <input id="diff-show-changes" name="original" type="checkbox" <#if original?exists && !original>checked="checked"</#if> />
                 <label for="diff-show-changes"><@vrtx.msg code="versions.diff.show-changes" default="Show changes" /></label>
               </form>
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
      </div>
      ${content}
    </body>
  </#if>
  
</html>