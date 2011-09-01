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
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/plugins/jquery.scrollTo-1.4.2-min.js"></script>
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
         e.preventDefault();
       });
              
                          // Params: class, appendTo, containerWidth, in-, pre-, outdelay, xOffset, yOffset, autoWidth
       $(".tree-create").vortexTips("li a", ".vrtx-create-tree", 80, 300, 4000, 300, 80, -8, true);
       
       // Traverse tree (TODO: recursively expand nodes from this array in jquery.treeview.async.js instead)
       var treeTrav = [<#list uris as link>"${link?html}"<#if uris[link_index+1]?exists>,</#if></#list>];
       var windowTree = $("#TB_ajaxContent .tree-create");
       if(treeTrav.length > 1) { // Ignore if only root
         var i = 1; // Skip root
         var tries = 200; // Only check for 200 * 15ms = 3s
         var checkLinkAvailable = setInterval(function() {
           var link = windowTree.find("a[href$='" + treeTrav[i] + "']");  
           if(link.length) {
             var hit = link.closest("li").find("> .hitarea");
             hit.click();
             if(i == (treeTrav.length-1)) { // Last uri
               // Scroll to
               $('#TB_ajaxContent').scrollTo(link, 250, {
                 easing: 'swing',
                 queue: true,
                 axis: 'y'
               });
               // Trigger mouseover (and make sure title is kept)
               /*
               var title = link.attr("title");
               link.trigger('mouseover');
               $(".tree-create").delegate("li a", "mouseover mouseleave", function (e) {
                if (e.type == "mouseover") {
                  link.attr("title", title);
                } else if (e.type == "mouseleave") {
                  link.attr("title", title);
                }
               });
               */
               clearInterval(checkLinkAvailable);
             } else {
               i++; // next URI
             }
           }
           if(!tries) {  
             clearInterval(checkLinkAvailable);
           }
           tries--;
         }, 15);
       }
     });
  // -->
  </script>
</head>
<body>
  <div class="vrtx-create-tree">
    <ul id="tree" class="filetree treeview-gray tree-create"></ul>
  </div>
</body>
</html>