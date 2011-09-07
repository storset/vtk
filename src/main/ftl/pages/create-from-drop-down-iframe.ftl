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
  <style type="text/css">
     #createTreeIframe {
       overflow:visible;
       width:100%;
       height: 395px;
     }
  </style>
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <script type="text/javascript"><!--
    // Show close link
    $("#TB_closeAjaxWindow").addClass("create-tree-close-window");
  // -->
  </script>
</head>
<body>
   <#if serviceUri == "create-document">
     <#assign url = docUrl.url />
   <#elseif serviceUri == "create-collection">
     <#assign url = collUrl.url />   
   <#elseif serviceUri == "upload-file">
     <#assign url = upUrl.url />  
   </#if>
   <#if !url?contains(":9322") && url?contains("http:")>
      <#assign url = url?replace("http:", "https:") />
   </#if>
   <iframe name="createTreeIframe" id="createTreeIframe" src="${url}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0">
      [Your browser does not support frames or is currently configured
      not to display frames.]
   </iframe>
   
</body>
</html>