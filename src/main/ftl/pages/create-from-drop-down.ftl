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
     $(document).ready(function(){
       var timestamp = 1 - new Date();
       $("#tree").treeview({
         animated: "fast",
         url: "?vrtx=admin&service=create-${type}-from-drop-down&uri=/&ts=" + timestamp,
         service: "create-${type}-from-drop-down"
       })

       $("#tree").delegate("a", "click", function(e){
           // don't want click on links
           return false;
       });
       
       // Bruke denne:
       // var treeTrav = [<#list uris as link>"${link?html}"<#if uris[link_index+1]?exists>,</#if></#list>];
       
       // Evt noe slikt:
       // <#list uris as link>
       //   Funksjon som bruker ${link?html}
       //  </#list>
       
     });
  // -->
  </script>
</head>
<body>
  <div class="vrtx-report vrtx-permission-tree">
    <ul id="tree" class="filetree treeview-gray"></ul>
  </div>
</body>
</html>
