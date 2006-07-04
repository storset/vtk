<#ftl strip_whitespace=true>

<#--
  - File: permissions.ftl
  - 
  - Description: A HTML page that displays the permissions set on a
  - resource
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/permissions.ftl" as permissions />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Permissions on resource</title>
</head>
<body>

  <div class="resourceInfo">

    <h2><@vrtx.msg code="permissions.header" default="Permissions on this resource" /></h2>
    <p><@permissions.editInheritance />
    <#if resourceContext.currentResource.acl.inherited>
      <@vrtx.msg code="permissions.isInherited" default="Inherited permissions" />
      <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setCustom" default="edit" /></a>&nbsp;)</#if>
      <#else>
        <@vrtx.msg code="permissions.notInherited" default="Custom permissions" />
        <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setInherited" default="edit" /></a>&nbsp;)</#if>
      </#if>
      </p>

      <h3><@vrtx.msg code="permissions.privilege.read" default="Read" /></h3>
      <div><@permissions.editOrDisplay 'read' /></div>

      <h3><@vrtx.msg code="permissions.privilege.write" default="Write" /></h3>
      <div><@permissions.editOrDisplay 'write' /></div>

      <h3><@vrtx.msg code="permissions.privilege.all" default="Admin" /></h3>
      <div><@permissions.editOrDisplay 'all' /></div>

      <h3 style="margin-top:1em;"><@vrtx.msg code="permissions.advanced" default="Advanced permissions" /></h3>
      <div class="smaller">
        <@vrtx.msg code="permissions.privilege.bind" default="Create resources only" />:
        <@permissions.editOrDisplay 'bind' />
      </div>
      <div class="smaller"><@vrtx.msg code="permissions.privilege.read-processed" default="Read processed only" />:
          <@permissions.editOrDisplay 'read-processed' />
      </div>
      <!-- table>
        <tr>
          <td><@vrtx.msg code="permissions.privilege.bind" default="Create resources only" />:</td>
          <td><@permissions.editOrDisplay 'bind' /></td>
        </tr>
        <tr>
          <td><@vrtx.msg code="permissions.privilege.read-processed" default="Read processed only" />:</td>
          <td><@permissions.editOrDisplay 'read-processed' /></td>
        </tr>
      </table -->

  </div>

</body>
</html>
