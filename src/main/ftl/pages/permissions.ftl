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

    <#assign resource = resourceContext.currentResource />
    <#assign defaultHeader = vrtx.getMsg("permissions.header", "Permissions on this resource") />

    <div class="resourceInfo permissions">
      <h2>
        <@vrtx.msg
           code="permissions.header.${resource.resourceType}"
           default="${defaultHeader}"/>
      </h2>

      <#assign defaultNotInherited = vrtx.getMsg("permissions.notInherited", "Custom permissions") />

      <p>
        <#if resourceContext.currentResource.inheritedAcl>
          <@vrtx.msg code="permissions.isInherited" default="Inherited permissions" />
          <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setCustom" default="edit" /></a>&nbsp;)</#if>
        <#else>
          <@vrtx.msg code="permissions.notInherited.${resource.resourceType}" default="${defaultNotInherited}" />
          <#if aclInfo.aclEditURLs.inheritance?exists>(&nbsp;<a href="${aclInfo.aclEditURLs.inheritance?html}"><@vrtx.msg code="permissions.setInherited" default="edit" /></a>&nbsp;)</#if>
        </#if>
      </p>

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.read" default="Read" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="read" privilegeHeading=privilegeHeading />

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.write" default="Write" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="write" privilegeHeading=privilegeHeading />

      <#assign privilegeHeading><@vrtx.msg code="permissions.privilege.all" default="Admin - all privileges" /></#assign>
      <@permissions.editOrDisplayPrivilege privilegeName="all" privilegeHeading=privilegeHeading />
     
      <#assign groupHeading><@vrtx.msg code="permissions.advanced" default="Advanced permissions" /></#assign>

      <#assign commentHeading><@vrtx.msg code="permissions.privilege.add-comment" default="Add comments" /></#assign>
      <#assign bindHeading><@vrtx.msg code="permissions.privilege.bind" default="Create resources only" /></#assign>
      <#assign readProHeading><@vrtx.msg code="permissions.privilege.read-processed" default="Read processed only" /></#assign>
      <#if resource.collection>
        <#assign privilegeList = [{"name":"add-comment", "heading":commentHeading }, {"name":"bind", "heading": bindHeading}, {"name":"read-processed", "heading":readProHeading }] />
      <#else>
        <#assign privilegeList = [{"name":"add-comment", "heading":commentHeading }, {"name":"read-processed", "heading":readProHeading }] />
      </#if>        
      <@permissions.editOrDisplayPrivileges privilegeList=privilegeList heading=groupHeading />

    </div>
  </body>
</html>

