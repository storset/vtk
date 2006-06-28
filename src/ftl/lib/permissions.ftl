<#ftl strip_whitespace=true>
<#--
  - File: permissions.ftl
  - 
  - Description: Permissions listing view component.
  - 
  - Required model data:
  -   resourceContext
  -  
  - Optional model data:
  -
  -->

<#import "/spring.ftl" as spring />
<#import "dump.ftl" as dumper />

<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>

<#macro editInheritance>
  <#if aclInheritanceForm?exists && !aclInheritanceForm.done>
    <tr class="inheritance">
      <td colspan="2" class="expandedForm">
        <form action="${aclInheritanceForm.submitURL?html}" method="POST">
          <h3>
            <@vrtx.msg
               code="permissions.header.${resourceContext.currentResource.resourceType}"
               default="defaultHeader"/>
          </h3>
          <ul class="inheritance">
            <li>
              <input id="permissions.isInherited" type="radio" name="inherited" value="true"
                     <#if aclInfo.inherited>checked="checked"</#if>>
              <label for="permissions.isInherited"><@vrtx.msg code="permissions.isInherited" default="Inherited"/></label>
            </li>
            <li>
              <input id="permissions.notInherited" type="radio" name="inherited" value="false"
                     <#if !aclInfo.inherited>checked="checked"</#if>>
              <label for="permissions.notInherited"><@vrtx.msg code="permissions.notInherited" default="Custom"/></label>
            </li>
          </ul>
          <input type="submit" name="saveAction" value="<@vrtx.msg code="permissions.save" default="Save"/>">
          <input type="submit" name="cancelAction" value="<@vrtx.msg code="permissions.cancel" default="Cancel"/>">
        </form>
      </td>
    </tr>
  </#if>

</#macro>


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
<#macro listPrincipals pseudoPrincipals users groups groupingPrincipal>
  <#assign grouped = false />
  <#list pseudoPrincipals as pseudoPrincipal>
    <#if pseudoPrincipal.name = groupingPrincipal.name>
      <#assign grouped = true />
    </#if>
  </#list>
  <#if grouped>
    <@vrtx.msg code="permissions.allowedFor.${groupingPrincipal.name}" default="${groupingPrincipal.name}" /><#t/>
  <#else>
    <#list pseudoPrincipals as pseudoPrincipal>
      <#compress>
        <@vrtx.msg code="pseudoPrincipal.${pseudoPrincipal.name}" default="${pseudoPrincipal.name}" /><#t/>
        <#if pseudoPrincipal.name = "pseudo:owner">&nbsp;(${resourceContext.currentResource.owner})</#if><#t/>
      </#compress>
      <#if pseudoPrincipal_index &lt; pseudoPrincipals?size - 1  || users?size &gt; 0  || groups?size &gt; 0>, <#t/></#if>
    </#list>
    <#list users as user>
      <#compress>${user.name}</#compress><#t/>
      <#if user_index &lt; users?size - 1 || groups?size &gt; 0>,<#t/></#if>
    </#list>
    <#list groups as group>
      <#compress>${group.name}</#compress><#t/>
      <#if group_index &lt; groups?size - 1>,<#t/></#if>
    </#list>
  </#if>
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
<#macro editACLForm formName privilege groupingPrincipal>
  <#-- 
    Should we use this to access form without having to do @spring.bind all the time?
    assign form=.vars[formName] /
  -->

   <#--assign grouped = acl.containsEntry(privilege, groupingPrincipal) /-->
   <#assign grouped = false />

  <@spring.bind formName + ".submitURL" /> 
  <div class="expandedForm" style="clear: both;">
  <form class="aclEdit" action="${spring.status.value?html}" method="POST">
    <ul class="everyoneOrSelectedUsers">
    <@spring.bind formName + ".grouped" /> 
    <#assign grouped = spring.status.value />
    <li>
      <input onclick="disableInput()"
             id="permissions.grouped"
             type="radio"
             name="${spring.status.expression}"
             value="true" <#if spring.status.value>checked="checked"</#if>> 
      <label for="permissions.everyone">
        <@vrtx.msg code="pseudoPrincipal.${groupingPrincipal.name}"
                   default="${groupingPrincipal.name}" /></label>
    </li>
    <li><input onclick="enableInput()"
               id="permissions.selectedPrincipals"
               type="radio"
               name="${spring.status.expression}"
               value="false"
               <#if !spring.status.value>checked="checked"</#if>> 
      <label for="permissions.selectedPrincipals">
        <@vrtx.msg code="permissions.selectedPrincipals" default="Selected users and groups"/></label>
    </li>
    </ul>

    <ul class="principalList" id="principalList">
      <li class="users">
      <fieldset>
      <legend><@vrtx.msg code="permissions.users" default="Users"/></legend>
      <ul class="users">
      <@spring.bind formName + ".removeUserURLs" />
      <#assign removeUserURLs=spring.status.value />
      <@spring.bind formName + ".users" />
      <#list spring.status.value as user>
          <#switch user.name>
            <#case "pseudo:owner">
              <li><@vrtx.msg code="permissions.owner"
                             default="owner"/>&nbsp;(${resourceContext.currentResource.owner.name})</li>
              <#break>
            <#case "pseudo:authenticated">
            <#case "pseudo:all">
              <#if user.name != groupingPrincipal.name>
                <li><@vrtx.msg code="pseudoPrincipal.${user.name}" default="${user.name}" /><#t/>
                  <#if removeUserURLs?exists && removeUserURLs[user.name]?exists >
                    &nbsp;(&nbsp;<a href="${removeUserURLs[user.name]?html}"><#t/>
                      <#t/><@vrtx.msg code="permissions.remove" default="remove"/></a>&nbsp;)
                  </#if>
              </#if>
              <#break>
            <#default>
              <li>${user.name}
                <#if removeUserURLs?exists && removeUserURLs[user.name]?exists >
                  &nbsp;(&nbsp;<a href="${removeUserURLs[user.name]?html}"><#t/>
                  <#t/><@vrtx.msg code="permissions.remove" default="remove"/></a>&nbsp;)
               </#if>
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
      <span class="addUser">
        <input type="text" size="15"
               name="${spring.status.expression}"
               value="${value?if_exists}">&nbsp;
	<input type="submit" name="addUserAction"
               value="<@vrtx.msg code="permissions.addUser" default="Add User"/>"/>
      </span>
      </fieldset>
      </li>
      <li class="groups">
      <fieldset>
      <legend><@vrtx.msg code="permissions.groups" default="Groups"/></legend>
      <ul class="groups">
      <@spring.bind formName + ".removeGroupURLs" />
      <#assign removeGroupURLs=spring.status.value />
      <@spring.bind formName + ".groups" /> 
      <#list spring.status.value as group>
        <li>
          <#compress>
          ${group}&nbsp;(&nbsp;<a href="${removeGroupURLs[group.name]?html}"><#t/>
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
	<input type="text" size="15"
               name="${spring.status.expression}"
               value="${value?if_exists}">&nbsp;
        <input type="submit" name="addGroupAction"
               value="<@vrtx.msg code="permissions.addGroup" default="Add Group"/>"/>
      </span>
    </fieldset>
    </li>
    </ul>
    <#-- Disable input if 'everyone' has permission: -->
    <#if grouped><script type="text/javascript">disableInput()</script></#if>

    <div id="submitButtons" class="submitButtons">
    <#-- Move buttons if 'everyone' has permission: -->
    <#if grouped><script type="text/javascript">document.getElementById("submitButtons").style.padding-top="5px";</script></#if>
    <input type="submit" name="saveAction" value="<@vrtx.msg code="permissions.save" default="Save"/>">
    <input type="submit" name="cancelAction" value="<@vrtx.msg code="permissions.cancel" default="Cancel"/>">
    </div>
  </form>	
  </div>
</#macro>



<#macro editOrDisplay privilegeName>
  <#assign formName = 'permissionsForm_' + privilegeName />
  <#assign privilege = aclInfo.privileges[privilegeName] />
  <#assign pseudoPrincipals = aclInfo.privilegedPseudoPrincipals[privilegeName] />
  <#assign users = aclInfo.privilegedUsers[privilegeName] />
  <#assign groups = aclInfo.privilegedGroups[privilegeName] />
  
  <#assign groupingPrincipal = aclInfo.groupingPrivilegePrincipalMap[privilegeName] />

  <#if .vars[formName]?exists>
    <@editACLForm
       formName = formName
       privilege = privilege
       groupingPrincipal = groupingPrincipal />
  <#else>
    <@listPrincipals
       pseudoPrincipals = pseudoPrincipals
       users = users
       groups = groups
       groupingPrincipal = groupingPrincipal />

    <#if aclInfo.aclEditURLs[privilegeName]?exists>(&nbsp;<a href="${aclInfo.aclEditURLs[privilegeName]?html}"><@vrtx.msg code="permissions.privilege.edit" default="edit" /></a>&nbsp;)</#if>
  </#if>
</#macro>


