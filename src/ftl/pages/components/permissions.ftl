<#ftl strip_whitespace=true>

<#--
  - File: permissions.ftl
  - 
  - Description: Permissions listing view component.
  - 
  - Required model data:
  -   aclInfo
  -   resourceContext
  -  
  - Optional model data:
  -   editReadPermissionsForm
  -   editWritePermissionsForm
  -   editWriteACLPermissionsForm
  -   aclInheritanceForm
  -
  -->


<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if !aclInfo?exists>
  <#stop "Unable to render model: required submodel
  'aclInfo' missing">
</#if>

<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>


<#--
 * listPrincipals
 *
 * Lists users and groups that have permissions on the current
 * resource. Owners are displayed with a '(owner') suffix.
 *
 * @param users list of users
 * @param groups list of groups
 *
-->
<#macro listPrincipals users groups>
  <#list users as user>
    <#switch user.name>
      <#case "dav:owner">
        <#compress>
        ${resourceContext.currentResource.owner.name} (<@vrtx.msg code="permissions.owner" default="owner"/>)<#t/>
        </#compress>
        <#break>
      <#default>
        <#compress>${user.name}</#compress><#t/>
    </#switch>
    <#if user_index &lt; users?size - 1 || groups?size &gt; 0>,<#t/></#if>
  </#list>
  <#list groups as group>
    <#compress>${group}</#compress><#t/>
    <#if group_index &lt; groups?size - 1>,<#t/></#if>
  </#list>
</#macro>


<#--
 * editACLForm
 *
 * Macro for displaying the 'Edit ACL' form
 *
 * @param formName the name of the form
 * @param privilege the privilege to edit
 *
-->
<#macro editACLForm formName privilege>
  <#-- 
    Should we use this to access form without having to do @spring.bind all the time?
    assign form=.vars[formName] / 
  -->
  <@spring.bind formName + ".submitURL" /> 
  <form class="aclEdit" action="${spring.status.value?html}" method="POST">
    <h3 style="float:left; clear:both;"><@vrtx.msg code="permissions.privilege.${privilege}" default="Allowed to ${privilege}"/>:</h3>
    <ul class="everyoneOrSelectedUsers">
    <@spring.bind formName + ".everyone" /> 
    <li>
      <input onclick="disableInput()" id="permissions.everyone" type="radio" name="${spring.status.expression}" value="true" <#if spring.status.value>checked="checked"</#if>> 
      <label for="permissions.everyone"><@vrtx.msg code="permissions.everyone" default="Everyone"/></label>
    </li>
    <li><input onclick="enableInput()" id="permissions.selectedPrincipals" type="radio" name="${spring.status.expression}"
               value="false"
               <#if !spring.status.value>checked="checked"</#if>> 
      <label for="permissions.selectedPrincipals"><@vrtx.msg code="permissions.selectedPrincipals" default="Selected users and groups"/></label>
    </li>
    </ul>
    <#-- Need this to check wheter to show users and groups or not -->
    <#assign everyone = spring.status.value/>

    <ul class="principalList" id="principalList">
      <li class="users">
      <fieldset>
      <legend><@vrtx.msg code="permissions.users" default="Users"/></legend>
      <ul class="users">
      <@spring.bind formName + ".withdrawUserURLs" />
      <#assign withdrawUserURLs=spring.status.value />
      <@spring.bind formName + ".users" />
      <#list spring.status.value as user>
          <#switch user>
            <#case "dav:authenticated">
              <#break>
            <#case "dav:owner">
              <li>${resourceContext.currentResource.owner.name}&nbsp;(<@vrtx.msg code="permissions.owner" default="owner"/>)</li>
              <#break>
            <#default>
              <li>${user.name}&nbsp;(&nbsp;<a href="${withdrawUserURLs[user_index]?html}"><#t/>
                  <#t/><@vrtx.msg code="permissions.remove" default="remove"/></a>&nbsp;)
              </li>
          </#switch>
        </#list>
      </ul>
      <@spring.bind formName + ".userNames" /> 
      <#assign value=""/>
      <#if spring.status.errorMessages?size &gt; 0>
      <ul class="errors">
      <#list spring.status.errorMessages as error> 
        <li>${error}</li> 
      </#list>
      </ul>
      <#if spring.status.value?exists>
        <#list spring.status.value as name>
          <#assign value=value + name/>
          <#if name_index+1 < spring.status.value?size>
            <#assign value=value + ","/>
          </#if>
        </#list>
      </#if>
      </#if>
      <span class="addUser"><input type="text" size="15" name="${spring.status.expression}" value="${value?if_exists}">&nbsp;
	<input type="submit" name="addUserAction" value="<@vrtx.msg code="permissions.addUser" default="Add User"/>"/>
      </span>
      </fieldset>
      </li>
      <li class="groups">
      <fieldset>
      <legend><@vrtx.msg code="permissions.groups" default="Groups"/></legend>
      <ul class="groups">
      <@spring.bind formName + ".withdrawGroupURLs" />
      <#assign withdrawGroupURLs=spring.status.value />
      <@spring.bind formName + ".groups" /> 
      <#list spring.status.value as group>
        <li>
          <#compress>
          ${group}&nbsp;(&nbsp;<a href="${withdrawGroupURLs[group_index]?html}"><#t/>
            <#t/><@vrtx.msg code="permissions.remove" default="remove"/></a>&nbsp;)
          </#compress>
        </li>
      </#list>
      </ul>
      <@spring.bind formName + ".groupNames" /> 
      <#assign value=""/>
      <#if spring.status.errorMessages?size &gt; 0>
      <ul class="errors">
      <#list spring.status.errorMessages as error> 
        <li>${error}</li> 
      </#list>
      </ul>
      <#if spring.status.value?exists>
        <#list spring.status.value as name>
          <#assign value=value + name/>
          <#if name_index+1 < spring.status.value?size>
            <#assign value=value + ","/>
          </#if>
        </#list>
      </#if>
      </#if>
      <span class="addGroup">
	<input type="text" size="15" name="${spring.status.expression}" value="${value?if_exists}">&nbsp;<input type="submit" name="addGroupAction" value="<@vrtx.msg code="permissions.addGroup" default="Add Group"/>"/>
      </span>
    </fieldset>
    </li>
    </ul>
    <#-- Disable input if 'everyone' has permission: -->
    <#if everyone><script type="text/javascript">disableInput()</script></#if>

    <div id="submitButtons" class="submitButtons">
    <#-- Move buttons if 'everyone' has permission: -->
    <#if everyone><script type="text/javascript">document.getElementById("submitButtons").style.padding-top="5px";</script></#if>
    <input type="submit" name="saveAction" value="<@vrtx.msg code="permissions.save" default="Save"/>">
    <input type="submit" name="cancelAction" value="<@vrtx.msg code="permissions.cancel" default="Cancel"/>">
    </div>
  </form>	
</#macro>

<div class="resourceInfoHeader" style="padding-bottom:1.5em;padding-top:0px;">
  <#assign defaultHeader = vrtx.getMsg("permissions.header", "Permissions on this resource") />
  <#assign defaultNotInherited = vrtx.getMsg("permissions.notInherited", "Custom") />
  <h2 class="permissionsToggleHeader">
    <@vrtx.msg
       code="permissions.header.${resourceContext.currentResource.contentType}"
       default="${defaultHeader}"/>
  </h2>
  <div class="permissionsToggleAction">
    <#if aclInfo.aclInheritedFrom?exists>
      <@vrtx.msg code="permissions.isInherited" default="Inherited"/>
    <#else>
      <@vrtx.msg code="permissions.notInherited.${resourceContext.currentResource.contentType}" default="${defaultNotInherited}"/>
    </#if>
    <#if aclInfo.aclInheritanceServiceURL?exists &&
      resourceContext.currentResource.URI != '/'>
      <span style="font-weight:normal;">(&nbsp;<a href="${aclInfo.aclInheritanceServiceURL?html}"><@vrtx.msg code="permissions.editInheritance" default="edit"/></a>&nbsp;)</span>
    </#if>
  </div>
</div>
<table style="clear:both" class="resourceInfo">

<#if aclInheritanceForm?exists && !aclInheritanceForm.done>
  <tr class="inheritance">
  <td colspan="2" class="expandedForm">
  <form action="${aclInheritanceForm.submitURL?html}" method="POST">
    <h3>
      <@vrtx.msg
         code="permissions.header.${resourceContext.currentResource.contentType}"
         default="${defaultHeader}"/>
    </h3>
    <ul class="inheritance">
    <li>
      <input id="permissions.isInherited" type="radio" name="inherited" value="true"
               <#if aclInfo.aclInheritedFrom?exists>checked="checked"</#if>>
      <label for="permissions.isInherited"><@vrtx.msg code="permissions.isInherited" default="Inherited"/></label>
    </li>
    <li>
      <input id="permissions.notInherited" type="radio" name="inherited" value="false"
             <#if !aclInfo.aclInheritedFrom?exists>checked="checked"</#if>>
      <label for="permissions.notInherited"><@vrtx.msg code="permissions.notInherited" default="Custom"/></label>
    </li>
    </ul>
    <input type="submit" name="saveAction" value="<@vrtx.msg code="permissions.save" default="Save"/>">
    <input type="submit" name="cancelAction" value="<@vrtx.msg code="permissions.cancel" default="Cancel"/>">
    </form>
  </td>
  </tr>
</#if>

<tr class="readPermissions">
  <#if editReadPermissionsForm?exists>
  <td colspan="2" class="expandedForm">
    <@editACLForm formName="editReadPermissionsForm" privilege="read" />
  </td>
  <#else>
  <td class="key">
    <@vrtx.msg code="permissions.privilege.read" default="Allowed to read"/>:
  </td>
  <td class="value">
    <#if aclInfo.everyoneReadAuthorized>
      <@vrtx.msg code="permissions.everyone" default="everyone"/>
    <#else>
      <@listPrincipals users=aclInfo.readAuthorizedUsers groups=aclInfo.readAuthorizedGroups />
    </#if>
    <#if !aclInfo.aclInheritedFrom?exists &&
         aclInfo.editReadPermissionsServiceURL?exists>
      (&nbsp;<a href="${aclInfo.editReadPermissionsServiceURL?html}"><@vrtx.msg code="permissions.privilege.read.edit" default="edit"/></a>&nbsp;)
    </#if>
  </td>
  </#if>
</tr>
<tr class="writePermissions">
  <#if editWritePermissionsForm?exists>
  <td colspan="2" class="expandedForm">
    <@editACLForm formName="editWritePermissionsForm" privilege="write" />
  </td>
  <#else>
  <td class="key">
    <@vrtx.msg code="permissions.privilege.write" default="Allowed to write"/>:
  </td>
  <td class="value">
    <#if aclInfo.everyoneWriteAuthorized>
      <@vrtx.msg code="permissions.everyone" default="Everyone"/>
    <#else>
      <@listPrincipals users=aclInfo.writeAuthorizedUsers groups=aclInfo.writeAuthorizedGroups />
    </#if>
    <#if !aclInfo.aclInheritedFrom?exists &&
         aclInfo.editWritePermissionsServiceURL?exists>
      (&nbsp;<a href="${aclInfo.editWritePermissionsServiceURL?html}"><@vrtx.msg code="permissions.privilege.write.edit" default="edit"/></a>&nbsp;)
    </#if>
  </td>
  </#if>
</tr>
<tr class="writeACLPermissions">
  <#if editWriteACLPermissionsForm?exists>
  <td colspan="2" class="expandedForm">
    <@editACLForm formName="editWriteACLPermissionsForm" privilege="write-acl" />
  </td>
  <#else>
  <td class="key">
    <@vrtx.msg code="permissions.privilege.write-acl" default="Allowed toset ACL"/>:
  </td>
  <td class="value">
    <#if aclInfo.everyoneWriteAclAuthorized>
      <@vrtx.msg code="permissions.everyone" default="Everyone"/>
    <#else>
      <@listPrincipals users=aclInfo.writeAclAuthorizedUsers groups=aclInfo.writeAclAuthorizedGroups />
    </#if>
    <#if !aclInfo.aclInheritedFrom?exists &&
         aclInfo.editWriteACLPermissionsServiceURL?exists>
      (&nbsp;<a href="${aclInfo.editWriteACLPermissionsServiceURL?html}"><@vrtx.msg code="permissions.privilege.write-acl.edit" default="edit"/></a>&nbsp;)
    </#if>
  </td>
  </#if>
</tr>
</table>

