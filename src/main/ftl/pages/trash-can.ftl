
<#ftl strip_whitespace=true>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Trash Can</title>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
    </#list>
  </#if>
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/ie6-table-tr-hover.js"></script>
  </head>
<body id="vrtx-trash-can">

<#-- WTF???? Why the F*** does this work??? -->
<#-- Without it, stupid freemarker puts spaces between digits in numbers -->
<#setting number_format="0">
<#-- END WTF -->

<@spring.bind "trashcan.trashCanObjects" />
<#if (spring.status.value?size > 0) >
<@spring.bind "trashcan.submitURL" />
<form class="trashcan" action="${spring.status.value?html}" method="post">

  <#-- Validation -->
  <@spring.bind "trashcan.trashCanObjects" />
  <#assign recoverableResources = spring.status.value />
  <#if spring.status.errorMessages?size &gt; 0>
  <div class="errorContainer">
    <ul class="errors">
    <#list spring.status.errorMessages as error> 
      <li>${error}</li> 
    </#list>
    </ul>
  </div>
  </#if>

  <table id="vrtx-trash-can-table" class="directoryListing">
    <tr id="vrtx-trash-can-header" class="directoryListingHeader">
      <th id="vrtx-trash-can-name"><@vrtx.msg code="trash-can.name" default="Name" /></th>
      <th></th>
      <th id="vrtx-trash-can-deleted-by"><@vrtx.msg code="trash-can.deletedBy" default="Deleted by" /></th>
      <th id="vrtx-trash-can-deleted-time"><@vrtx.msg code="trash-can.deletedTime" default="Deleted time" /></th>
    </tr>

    <#list spring.status.value as tco>
    <#if (tco_index % 2 == 0)>
      <tr class="odd ${tco.recoverableResource.resourceType}">
    <#else>
      <tr class="even ${tco.recoverableResource.resourceType}">
    </#if>
        <td class="vrtx-trash-can-name name"><a href=".">${tco.recoverableResource.name?html}</a></td>
        <td>
        <@spring.bind "trashcan.trashCanObjects[${tco_index}].selectedForRecovery" />
        <#assign checked = "" />
        <#if spring.status.value?string = 'true' >
          <#assign checked = "checked" />
        </#if>
        <input type="checkbox" name="${spring.status.expression}" value="true" ${checked} />
        </td>
        <td class="vrtx-trash-deleted-by"><@vrtx.displayUserPrincipal principal=tco.recoverableResource.deletedBy /></td>
        <td class="vrtx-trash-deleted-time"><@printDeletedTime tco.recoverableResource.deletedTime /></td>
      </tr>
    </#list>

  </table>
  <input class="recoverResource" type="submit" name="recoverResourceAction"
               value="<@vrtx.msg code="trash-can.recover" default="Recover"/>"/>
</form>
<#else>
  <@vrtx.msg code="trash-can.empty" default="The trash can contains no garbage." />
</#if>

</body>
</html>

<#macro printDeletedTime time>
  ${time?string("yyyy-MM-dd HH:mm:ss")}
</#macro>