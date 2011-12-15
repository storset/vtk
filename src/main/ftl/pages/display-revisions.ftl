<#ftl strip_whitespace=true>
<#--
  - File: display-revisions.ftl
  - 
  - Description: Lists revisions for a resource
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#macro changeAmount amount>
  <#if amount &gt; 9><#local amount = 10 /></#if>
  <#local rest = 10 - amount />
  <#if amount &gt; 0><#list 1..amount as i><span class="vrtx-revision-amount"></span></#list></#if><#rt />
  <#lt/><#if rest &gt; 0><#list 1..rest as i><span class="vrtx-revision-amount-rest"></span></#list></#if>
</#macro>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Revisions</title>
  </head>
  <body id="vrtx-revisions">
    <h2><@vrtx.msg code="versions.title" /></h2>

    <table class="resourceInfo revisions">
      <thead>
        <tr>
          <th><@vrtx.msg code="versions.table.title" />  #</th>
          <th><@vrtx.msg code="versions.table.modified-by" /></th>
          <th><@vrtx.msg code="versions.table.time" /></th>
          <th><@vrtx.msg code="versions.table.change-amount" /></th>
          <th></th>
        </tr>
      </head>
      <tbody>
        <#if workingCopy?exists>
          <tr id="vrtx-revisions-working-copy">
            <td><@vrtx.msg code="versions.table.working-copy" /></td>
            <td>${workingCopy.principal.description?html}</td>
            <td>${workingCopy.timestamp?datetime}</td>
            <td>
              <#if (workingCopy.changeAmount)?exists>
                <@changeAmount workingCopy.changeAmount />
              </#if>
            </td>
            <td class="vrtx-revisions-buttons-column">
              <#if (workingCopy.displayURL)?exists>
              <a class="vrtx-revisions-view vrtx-button-small" href="${workingCopy.displayURL?html}"><span><@vrtx.msg code="versions.table.buttons.view" /></span></a>
              </#if>
              <#if (workingCopy.deleteURL)?exists>
                <form action="${workingCopy.deleteURL?html}" method="post">
                  <div class="vrtx-button-small">
                    <input type="submit" value="${vrtx.getMsg("versions.table.buttons.delete")}" />
                  </div>
                </form>
              </#if>
              <#if (workingCopy.restoreURL)?exists>
                <form action="${workingCopy.restoreURL?html}" method="post">
                  <div class="vrtx-button-small">
                    <input type="submit" value="${vrtx.getMsg("versions.table.buttons.make-current")}" />
                  </div>
                </form>
              </#if>
            </td>
          </tr>
        </#if>
        <tr>
          <td id="vrtx-revisions-current"><strong><@vrtx.msg code="versions.table.current-version" /></strong></td>
          <td>${resource.modifiedBy.description?html}</td>
          <td>${resource.lastModified?datetime}</td>
          <td>
            <#if (resourceChangeAmount?exists)>
                <@changeAmount resourceChangeAmount />
            </#if>
          </td>
          <td class="vrtx-revisions-buttons-column">
            <a class="vrtx-revisions-view vrtx-button-small" href="${displayURL?html}"><span><@vrtx.msg code="versions.table.buttons.view" /></span></a>
          </td>
        </tr>
        <#assign number = regularRevisions?size />
        <#list regularRevisions as revision>
          <tr>
            <!-- ID: ${revision.id?c}, ACL:${revision.acl?html} -->
            <td><@vrtx.msg code="versions.table.entry.name" args=[revision.name] /></td>
            <td>${revision.principal.description?html}</td>
            <td>${revision.timestamp?datetime}</td>
            <td>
              <#if (revision.changeAmount)?exists>
                <@changeAmount revision.changeAmount />
              </#if>
            </td>
            <td class="vrtx-revisions-buttons-column">
              <#if (revision.displayURL)?exists>
                <a class="vrtx-revisions-view  vrtx-button-small" href="${revision.displayURL?html}"><span><@vrtx.msg code="versions.table.buttons.view" /></span></a>
              </#if>
              <#if (revision.deleteURL)?exists>
                <form action="${revision.deleteURL?html}" method="post">
                  <div class="vrtx-button-small">
                    <input type="submit" value="${vrtx.getMsg("versions.table.buttons.delete")}" />
                  </div>
                </form>
              </#if>
              <#if (revision.restoreURL)?exists>
                <form action="${revision.restoreURL?html}" method="post">
                  <div class="vrtx-button-small">
                    <input type="submit" value="${vrtx.getMsg("versions.table.buttons.restore")}" />
                  </div>
                </form>
              </#if>
            </td>
          </tr>
          <#assign number = number - 1 />
        </#list>
      </tbody>
    </table>
  </body>
</html>
