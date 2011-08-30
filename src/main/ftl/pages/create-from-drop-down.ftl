<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
    </#list>
  </#if>
  <!--[if lte IE 8]>
    <link rel="stylesheet" type="text/css" href="/vrtx/__vrtx/static-resources/themes/default/report/jquery.treeview.ie.css" />
  <![endif]-->
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <script type="text/javascript">
  <!--
     $(document).ready(function() {
       var timestamp = 1 - new Date();
       $(".tree-create").treeview({
         animated: "fast",
         url: "?vrtx=admin&service=${type}-from-drop-down&uri=/&ts=" + timestamp,
         service: "${type}-from-drop-down"
       })

       $(".tree-create").delegate("a", "click", function(e){
           // don't want click on links
           return false;
       });
              
       // Params: class, appendTo, containerWidth, in-, pre-, outdelay, xOffset, yOffset
       $(".tree-create").vortexTips("li a", "#contents", 100, 300, 4000, 3000, 350, -35);
       
       // Traverse tree
       var treeTrav = [<#list uris as link>"${link?html}"<#if uris[link_index+1]?exists>,</#if></#list>];
       
       // Cache create tree ref.
       var windowTree = $("#TB_ajaxContent .tree-create");
       
       if(treeTrav.length > 1) { // Ignore if only root
         var i = 1; // Skip root
         var checkLinkAvailable = setInterval(function() {
           var link = windowTree.find("a[href='" + treeTrav[i] + "']");
           if(link.length) {
             var hit = link.closest("li").find("> .hitarea");
             hit.click();
             if(i == (treeTrav.length-1)) {
               clearInterval(checkLinkAvailable);
             } else {
               i++; // next URI
             }
           }
         }, 15);
       }
       
     });
  // -->
  </script>
</head>
<body>
  <div class="vrtx-report vrtx-permission-tree">
    <ul id="tree" class="filetree treeview-gray tree-create"></ul>
  </div>
</body>
</html>