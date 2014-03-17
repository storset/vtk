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

<#--
<#macro changeAmount amount>
  <#if amount &gt; 9><#local amount = 10 /></#if>
  <#local rest = 10 - amount />
  <#if amount &gt; 0><#list 1..amount as i><span class="vrtx-revision-amount"></span></#list></#if><#rt />
  <#lt/><#if rest &gt; 0><#list 1..rest as i><span class="vrtx-revision-amount-rest"></span></#list></#if>
</#macro>
-->

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Revisions</title>
  </head>
  <body id="vrtx-revisions">
    <h2><@vrtx.msg code="versions.title" /></h2>
    <script type="text/javascript"><!--
      var versionsRestoredInfoMsg = "<@vrtx.msg code='versions.table.info-msg.restored-version' />";
      var versionsMadeCurrentInfoMsg = "<@vrtx.msg code='versions.table.info-msg.made-current-version' />";
    // -->
    </script>
    <table class="resourceInfo revisions">
      <thead>
        <tr>
          <th><@vrtx.msg code="versions.table.title" />  #</th>
          <th><@vrtx.msg code="versions.table.modified-by" /></th>
          <th><@vrtx.msg code="versions.table.time" /></th>
          <#-- <th><@vrtx.msg code="versions.table.change-amount" /></th> -->
          <th></th>
        </tr>
      </head>
      <tbody>
        <#if workingCopy?exists>
          <tr id="vrtx-revisions-working-copy">
            <td><@vrtx.msg code="versions.table.working-copy" /></td>
            <td>${workingCopy.principal.description?html}</td>
            <td><@vrtx.date value=workingCopy.timestamp format="longlong" /></td>
       <#-- <td>
              <#if (workingCopy.changeAmount)?exists>
                <@changeAmount workingCopy.changeAmount />
              </#if>
            </td> -->
            <td class="vrtx-revisions-buttons-column">
              <#if (workingCopy.displayURL)?exists>
                <a class="vrtx-revision-view vrtx-button-small" href="${workingCopy.displayURL?html}"><@vrtx.msg code="versions.table.buttons.view" /></a>
              </#if>
              <#if (workingCopy.diffURL)?exists>
                <a class="vrtx-revision-view-changes vrtx-button-small" href="${workingCopy.diffURL?html}"><@vrtx.msg code="versions.table.buttons.view-changes" /></a>
              </#if>
              <#if (workingCopy.deleteURL)?exists>
                <form action="${workingCopy.deleteURL?html}" method="post" class="vrtx-revisions-delete-form">
                  <input class="vrtx-button-small" type="submit" value="${vrtx.getMsg("versions.table.buttons.delete")}" />
                </form>
              </#if>
              <#if (workingCopy.restoreURL)?exists>
                <form action="${workingCopy.restoreURL?html}" method="post" id="vrtx-revisions-make-current-form">
                  <input class="vrtx-button-small" type="submit" value="${vrtx.getMsg("versions.table.buttons.make-current")}" />
                </form>
              </#if>
            </td>
          </tr>
        </#if>
        <tr>
          <td id="vrtx-revisions-current"><strong><@vrtx.msg code="versions.table.current-version" /></strong></td>
          <td>${resource.modifiedBy.description?html}</td>
          <td><@vrtx.date value=resource.lastModified format="longlong" /></td>
     <#-- <td>
            <#if (resourceChangeAmount?exists)>
                <@changeAmount resourceChangeAmount />
            </#if>
          </td>-->
          <td class="vrtx-revisions-buttons-column">
            <a class="vrtx-revision-view vrtx-button-small" href="${displayURL?html}"><@vrtx.msg code="versions.table.buttons.view" /></a>
            <#if (diffURL)?exists>
              <a class="vrtx-revision-view-changes vrtx-button-small" href="${diffURL?html}"><@vrtx.msg code="versions.table.buttons.view-changes" /></a>
            </#if>
          </td>
        </tr>
        <#assign number = regularRevisions?size />
        
        <#list regularRevisions as revision>
          <tr>
            <!-- ID: ${revision.id?c}, ACL:${revision.acl?html} -->
            <td><@vrtx.msg code="versions.table.entry.name" args=[revision.name] /></td>
            <td>${revision.principal.description?html}</td>
            <td><@vrtx.date value=revision.timestamp format="longlong" /></td>
       <#-- <td>
              <#if (revision.changeAmount)?exists>
                <@changeAmount revision.changeAmount />
              </#if>
            </td>-->
            <td class="vrtx-revisions-buttons-column">
              <#if (revision.displayURL)?exists>
                <a class="vrtx-revision-view vrtx-button-small" href="${revision.displayURL?html}"><@vrtx.msg code="versions.table.buttons.view" /></a>
              </#if>
              <#if (revision.diffURL)?exists>
                <a class="vrtx-revision-view-changes vrtx-button-small" href="${revision.diffURL?html}"><@vrtx.msg code="versions.table.buttons.view-changes" /></a>
              </#if>
              <#if (revision.deleteURL)?exists>
                <form action="${revision.deleteURL?html}" method="post" class="vrtx-revisions-delete-form">
                  <input class="vrtx-button-small" type="submit" value="${vrtx.getMsg("versions.table.buttons.delete")}" />
                </form>
              </#if>
              <#if (revision.restoreURL)?exists>
                <form action="${revision.restoreURL?html}" method="post" class="vrtx-revisions-restore-form">
                  <input class="vrtx-button-small" type="submit" value="${vrtx.getMsg("versions.table.buttons.restore")}" />
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
