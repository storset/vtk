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
    <style type="text/css">
       span.diff-html-removed {
             text-decoration: line-through;
       }
       span.diff-html-added {
             color: green;
       }
    </style>
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
      });
    // -->
    </script>
  </head>
  
  <#assign resource = resourceContext.currentResource />
  
  <body id="vrtx-${resource.resourceType}">
    <div id="vrtx-sticky-header">
      <div id="vrtx-sticky-header-inner">
         <span id="diff-header">Versjon ${revisionB?html}</span>
         <span id="diff-show-changes-info-nav">
           <span id="diff-show-changes-info">
             <form id="diff-show-changes-form" action="" method="get">
               <input id="diff-show-changes" name="diff-show-changes" type="checkbox" />
               <label for="diff-show-changes">Vis endringer</label>
             </form>
             <span id="diff-info">Endret av <span id="diff-info-modified-by">{modifisert-av}</span>, {modifisert-tid}</span>
           </span>
           <span id="diff-nav">
             <a id="diff-nav-prev" href="/{forrige-url}"><@vrtx.msg code="previous" default="Previous" /></a>
             <a id="diff-nav-next" href="/{neste-url}"><@vrtx.msg code="next" default="Next" /></a>
           </span>
         </span>
      </div>
    </div>
    
    ${content}
  </body>
</html>