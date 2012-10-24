<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
</head>
<body>
  <div>
  <div id="vrtx-manage-create-content">
  <div class="vrtx-create-tree">
    <ul id="tree" class="filetree treeview-gray tree-create"></ul>
  </div>
  
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
      <link rel="stylesheet" href="${cssURL}" type="text/css" />
    </#list>
  </#if>
  
  <!--[if lte IE 8]>
    <link rel="stylesheet" type="text/css" href="/vrtx/__vrtx/static-resources/themes/default/report/jquery.treeview.ie.css" type="text/css" />
  <![endif]-->
  
  <div id="vrtx-create-tree-folders"><#list uris as link>${link?html}<#if uris[link_index+1]?exists>,</#if></#list></div>
  <div id="vrtx-create-tree-type">${type}</div>
  
  </div></div>
</body>
</html>