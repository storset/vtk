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

  <div style="padding-left:0.5em;padding-right:0.5em;padding-bottom:1em;">
    <h2><@vrtx.msg code="permissions.header" default="Permissions on this resource" /></h2>
    <@permissions.editInheritance />
    <#if resourceContext.currentResource.acl.inherited>
      <@vrtx.msg code="permissions.isInherited" default="Inherited permissions" />
      <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setCustom" default="edit" /></a>&nbsp;)</#if>
      <#else>
        <@vrtx.msg code="permissions.notInherited" default="Custom permissions" />
        <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setInherited" default="edit" /></a>&nbsp;)</#if>
      </#if>

      <h3><@vrtx.msg code="permissions.privilege.read" default="Read" /></h3>
      <@permissions.editOrDisplay 'read' />

      <h3><@vrtx.msg code="permissions.privilege.write" default="Write" /></h3>
      <@permissions.editOrDisplay 'write' />

      <h3><@vrtx.msg code="permissions.privilege.all" default="Admin" /></h3>
      <@permissions.editOrDisplay 'all' />

      <h3><@vrtx.msg code="permissions.advanced" default="Advanced permissions" /></h3>
      <div><@vrtx.msg code="permissions.privilege.bind" default="Create resources only" />:
        <@permissions.editOrDisplay 'bind' />
      </div>
      <div><@vrtx.msg code="permissions.privilege.read-processed" default="Read processed only" />:
        <@permissions.editOrDisplay 'read-processed' />
      </div>

  </div>

</body>
</html>
