<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
</head>
<body>
  <div class="vrtx-create-tree">
    <ul id="tree" class="filetree treeview-gray tree-create"></ul>
  </div>
  
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
  <script type="text/javascript"><!--
    
    var treeTrav = [<#list uris as link>"${link?html}"<#if uris[link_index+1]?exists>,</#if></#list>];
    
    $(function() {
    
       $(".tree-create").css("display", "none");
       $("#TB_ajaxContent").css("background", "url(/vrtx/__vrtx/static-resources/js/plugins/thickbox-modified/loadingAnimation.gif) no-repeat center 158px");
    
       var timestamp = 1 - new Date();
       $("#TB_title").addClass("show-close-window");
       var pathNum = 0;
       $(".tree-create").treeview({
         animated: "fast",
         url: "?vrtx=admin&service=${type}-from-drop-down&uri=&ts=" + timestamp,
         service: "${type}-from-drop-down",
         dataLoaded: function() { // AJAX success
           var last = false;
           if (pathNum == (treeTrav.length-1)) {
             last = true;
           }
           traverseNode(treeTrav[pathNum++], last);
         }
       })

       $(".tree-create").on( "click", "a", function(e) { // Don't want click on links
         e.preventDefault();
       });
              
       // Params: class, appendTo, containerWidth, in-, pre-, outdelay, xOffset, yOffset, autoWidth
       $(".tree-create").vortexTips("li span.folder", ".vrtx-create-tree", 80, 300, 4000, 300, 10, -8, false, true);
     });
     
     function treeCreateScrollToCallback(link) {
       linkTriggeredMouseEnter = link;
       linkTriggeredMouseEnterTipText = linkTriggeredMouseEnter.attr('title');
       link.parent().trigger("mouseenter");
     }
     
     function traverseNode(treeTravNode, lastNode) {
       var windowTree = $(".tree-create");
       var checkNodeAvailable = setInterval(function() {
         var link = windowTree.find("a[href$='" + treeTravNode + "']");  
         if(link.length) {
           clearInterval(checkNodeAvailable);
           var hit = link.closest("li").find("> .hitarea");
           hit.click();
           if(lastNode) { // If last: scroll to node
             $("#TB_ajaxContent").css("background", "none");
             $(".tree-create").fadeIn(200, function() {
               var scrollToLink = (link.position().top - 145);
               scrollToLink = scrollToLink < 0 ? 0 : scrollToLink;
               $("#TB_ajaxContent").scrollTo(scrollToLink, 250, {
                 easing: "swing",
                 queue: true,
                 axis: 'y',
                 complete: treeCreateScrollToCallback(link)
               });
             });
           }
         }
       }, 15);
     }
  // -->
  </script>
</body>
</html>