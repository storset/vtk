<#ftl strip_whitespace=true />
<#import "/lib/vtk.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Collection listing</title>
    <#if jsURLs?exists>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
    <#include "/system/system.ftl" />
    <#if cssURLs?exists>
      <#list cssURLs as cssUrl>
        <link href="${cssUrl}" type="text/css" rel="stylesheet" />
      </#list>
    </#if>
  </head>
  <body class="embedded2 forms-new">
    <table id="directory-listing" class="collection-listing">
      <tbody> 
      <#list entries as entry>
        <#assign url = (entry.actions['view'])?default('') />
        <tr class="<#if entry_index % 2 == 0>odd<#else>even</#if> <@vrtx.resourceToIconResolver entry.resource /><#if entry.resource.collection> true</#if>">
          <td class="name"><a href="${url?html}">${entry.resource.title}</a></td>
          <#list [ "delete" ] as action>
            <#if !(entry.actions[action])?exists>
              <td></td>
            <#else>
              <td><a class="delete-action" href="${entry.actions[action]?html}">${action?html}</a></td>
            </#if>
          </#list>
        </tr>
      </#list>
      <tbody> 
    </table>

    <div id="upload-action-container">
      <span>&nbsp;</span>
    </div>

    <#list globalActions?keys as globalAction>
      <div class="globalaction">
        <a id="upload-action" href="${globalActions[globalAction]?html}">${globalAction?html}</a>
      </div>
    </#list>
  </body>
</html>