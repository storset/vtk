<#import "/lib/vtk.ftl" as vrtx />
<!DOCTYPE html>
<html>
  <head>
    <title>Collection listing</title>
  </head>
  <body>
    <table class="collection-listing">
      <#list entries as entry>
        <#assign url = (entry.actions['view'])?default('') />
        <tr class="${entry.resource.resourceType?html}">
        <td><a href="${url?html}">${entry.resource.title}</a></td>
        <#list [ "delete" ] as action>
          <#if !(entry.actions[action])?exists>
            <td></td>
          <#else>
            <td><a href="${entry.actions[action]?html}">${action?html}</a></td>
          </#if>
        </#list>
      </tr>
      </#list>
    </table>

    <#list globalActions?keys as globalAction>
      <div class="globalaction">
      <a href="${globalActions[globalAction]?html}">${globalAction?html}</a>
      </div>
    </#list>

  </body>
</html>
