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

    <div class="resourceInfo permissions">
      <h2><@vrtx.msg code="permissions.header" default="Permissions on this resource" /></h2>
      <p>
        <#if resourceContext.currentResource.acl.inherited>
          <@vrtx.msg code="permissions.isInherited" default="Inherited permissions" />
          <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setCustom" default="edit" /></a>&nbsp;)</#if>
        <#else>
          <@vrtx.msg code="permissions.notInherited" default="Custom permissions" />
          <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setInherited" default="edit" /></a>&nbsp;)</#if>
        </#if>
      </p>

     
      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.read" default="Read" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="read" privilegeHeading=privilegeHeading />

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.write" default="Write" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="write" privilegeHeading=privilegeHeading />

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.all" default="Admin" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="all" privilegeHeading=privilegeHeading />


      <#assign groupHeading><@vrtx.msg code="permissions.advanced" default="Advanced permissions" /></#assign>
      <#assign bindHeading><@vrtx.msg code="permissions.privilege.bind" default="Create resources only" /></#assign>
      <#assign readProHeading><@vrtx.msg code="permissions.privilege.read-processed" default="Read processed only" /></#assign>
      <#assign privilegeList = [{"name":"bind", "heading": bindHeading}, {"name":"read-processed", "heading":readProHeading }] />
      <@permissions.editOrDisplayPrivileges privilegeList = privilegeList heading = groupHeading />
    </div>
  </body>
</html>

