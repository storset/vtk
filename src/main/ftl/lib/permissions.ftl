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
<#import "/lib/autocomplete.ftl" as autocomplete />

<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>


<#--
 * editOrDisplayPrivilege
 *
 * Display privilege as a single group with heading.
 * 
 * @param privilegeName - name of privilege
 * @param privilegeHeading 
 *
-->

<#macro editOrDisplayPrivilege privilegeName privilegeHeading>
  <#assign formName = 'permissionsForm_' + privilegeName />

  <#if .vars[formName]?exists>
    <div>
      <div class="expandedForm">
        <@editACLFormNew
           formName = formName
           privilegeName = privilegeName
           privilegeHeading = privilegeHeading />
      </div>
    </div>
   <#else>

      <h3 class="${privilegeName}">${privilegeHeading}</h3>
      <div class="${privilegeName}"><@listPrincipals privilegeName=privilegeName /><#if aclInfo.aclEditURLs[privilegeName]?exists>(&nbsp;<a href="${aclInfo.aclEditURLs[privilegeName]?html}"><@vrtx.msg code="permissions.privilege.edit" default="edit" /></a>&nbsp;)</#if></div>
    
    </#if>
</#macro>


<#--
 * editOrDisplayPrivileges
 *
 * Display a list of privileges as one group with heading (table).
 * 
 * @param privilegeList - sequence of hashes with name of privilege, privilege heading
 * @param heading - name of group
 *
-->

<#macro editOrDisplayPrivileges privilegeList heading>
  <h3 class="privelegeList">${heading}</h3>
  <table>
    <#list privilegeList as p>
      <tr>
      <#assign formName = 'permissionsForm_' + p.name />
      <#assign privilegeName = p.name />
      <#assign privilegeHeading = p.heading />
      <#if .vars[formName]?exists>
        <td colspan="2" class="expandedForm">
        <@editACLFormNew
           formName = formName
           privilegeName = privilegeName 
           privilegeHeading = privilegeHeading />
        </td>
      <#else>
        <td class="key">${privilegeHeading}</td>
        <td>
             <@listPrincipalsSingleList        
                privilegeName = privilegeName />
             <#if aclInfo.aclEditURLs[privilegeName]?exists>(&nbsp;<a href="${aclInfo.aclEditURLs[privilegeName]?html}"><@vrtx.msg code="permissions.privilege.edit" default="edit" /></a>&nbsp;)</#if>
          </td>
      </#if>
      </tr>
    </#list>
  </table>
</#macro>


<#--
 * listPrincipalsSingleList
 *
 * Lists users and groups that have privileges on the current
 * resource as one list. Owners are displayed with a '(owner') suffix.
 *
 * @param privilegeName - name of privilege
 *
-->

<#macro listPrincipalsSingleList privilegeName>
  <#assign pseudoPrincipals = aclInfo.privilegedPseudoPrincipals[privilegeName] />
  <#assign groupingPrincipal = aclInfo.groupingPrivilegePrincipalMap[privilegeName] />
  <#assign users = aclInfo.privilegedUsers[privilegeName] />
  <#assign groups = aclInfo.privilegedGroups[privilegeName] />

  <#assign grouped = false />
  <#list pseudoPrincipals as pseudoPrincipal>
    <#if pseudoPrincipal.name = groupingPrincipal.name>
      <#assign grouped = true />
    </#if>
  </#list>

  <#if grouped>
    <@vrtx.msg code="permissions.allowedFor.${groupingPrincipal.name}" default="${groupingPrincipal.name}" /><#t/>
  <#elseif (pseudoPrincipals?size > 0 || users?size > 0 || groups?size > 0)>
    <#list pseudoPrincipals as pseudoPrincipal>
      <#compress>
        <@vrtx.msg code="pseudoPrincipal.${pseudoPrincipal.name}" default="${pseudoPrincipal.name}" /><#t/>
        <#if pseudoPrincipal.name = "pseudo:owner">&nbsp;(<@displayUserPrincipal principal=resourceContext.currentResource.owner />)</#if><#t/>
      </#compress>
      <#if pseudoPrincipal_index &lt; pseudoPrincipals?size - 1  || users?size &gt; 0  || groups?size &gt; 0>, <#t/></#if>
    </#list>
    <#list users as user>
      <#compress><@displayUserPrincipal principal=user /></#compress><#t/>
      <#if user_index &lt; users?size - 1 || groups?size &gt; 0>,<#t/></#if>
    </#list>
    <#list groups as group>
      <#compress>${group.name}</#compress><#t/>
      <#if group_index &lt; groups?size - 1>,<#t/></#if>
    </#list>
  <#else>
    <@vrtx.msg code="permissions.not.assigned" default="Not assigned" /> <#t/>
  </#if>
</#macro>


<#--
 * listPrincipalsSeparatedLists
 *
 * Lists users and groups that have privileges on the current
 * resource in two tablerows. Paragraph if "everyone" have privileges. 
 * Owners are displayed with a '(owner') suffix.
 *
 * @param privilegeName - name of privilege
 *
-->

<#macro listPrincipalsSeparatedLists privilegeName>
  <#assign pseudoPrincipals = aclInfo.privilegedPseudoPrincipals[privilegeName] />
  <#assign groupingPrincipal = aclInfo.groupingPrivilegePrincipalMap[privilegeName] />
  <#assign users = aclInfo.privilegedUsers[privilegeName] />
  <#assign groups = aclInfo.privilegedGroups[privilegeName] />

  <#assign grouped = false />
  <#list pseudoPrincipals as pseudoPrincipal>
    <#if pseudoPrincipal.name = groupingPrincipal.name>
      <#assign grouped = true />
    </#if>
  </#list>

  <#if grouped>
    <p><@vrtx.msg code="permissions.allowedFor.${groupingPrincipal.name}" default="${groupingPrincipal.name}" /><#t/></p>
  <#elseif (pseudoPrincipals?size > 0 || users?size > 0 || groups?size > 0)>
    <table>
      <tr>
        <td class="key"><@vrtx.msg code="permissions.users" default="Users"/>:</td>
        <td> 
          <#list pseudoPrincipals as pseudoPrincipal>
            <#compress>
              <@vrtx.msg code="pseudoPrincipal.${pseudoPrincipal.name}" default="${pseudoPrincipal.name}" /><#t/>
              <#if pseudoPrincipal.name = "pseudo:owner">&nbsp;(<@displayUserPrincipal principal=resourceContext.currentResource.owner />)</#if><#t/>
            </#compress>
            <#if pseudoPrincipal_index &lt; pseudoPrincipals?size - 1  || users?size &gt; 0>, <#t/></#if>
          </#list>
          <#list users as user>
            <#compress><@displayUserPrincipal principal=user /></#compress><#t/>
            <#if user_index &lt; users?size - 1>,<#t/></#if>
          </#list>
        </td>
      </tr>
      <tr>
        <td class="key"><@vrtx.msg code="permissions.groups" default="Groups"/>:</td>
        <td>
          <#list groups as group>
            <#compress>${group.name}</#compress><#t/>
            <#if group_index &lt; groups?size - 1>,<#t/></#if>
          </#list>
        </td>
      </tr>
    </table>
  <#else>
    <p><@vrtx.msg code="permissions.not.assigned" default="Not assigned" /><#t/></p>
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

<#macro listPrincipals privilegeName>
  <#assign pseudoPrincipals = aclInfo.privilegedPseudoPrincipals[privilegeName] />
  <#assign groupingPrincipal = aclInfo.groupingPrivilegePrincipalMap[privilegeName] />
  <#assign users = aclInfo.privilegedUsers[privilegeName] />
  <#assign groups = aclInfo.privilegedGroups[privilegeName] />

  <#assign grouped = false />
  <#list pseudoPrincipals as pseudoPrincipal>
    <#if pseudoPrincipal.name = groupingPrincipal.name>
      <#assign grouped = true />
    </#if>
  </#list>

  <#if grouped>
    <#compress>
      <@vrtx.msg code="permissions.allowedFor.${groupingPrincipal.name}" default="${groupingPrincipal.name}" />&nbsp;
    </#compress>
  <#elseif (pseudoPrincipals?size > 0 || users?size > 0 || groups?size > 0)>
    <#list pseudoPrincipals as pseudoPrincipal>
      <#compress>
        <@vrtx.msg code="pseudoPrincipal.${pseudoPrincipal.name}" default="${pseudoPrincipal.name}" />
        <#if pseudoPrincipal.name = "pseudo:owner">&nbsp;(<@displayUserPrincipal principal=resourceContext.currentResource.owner />)</#if><#t/>
      </#compress>
      <#if pseudoPrincipal_index &lt; pseudoPrincipals?size - 1  || users?size &gt; 0  || groups?size &gt; 0>, <#t/></#if>
    </#list>
    <#list users as user>
      <#compress><@displayUserPrincipal principal=user /></#compress><#t/>
      <#if user_index &lt; users?size - 1 || groups?size &gt; 0>,<#t/></#if>
    </#list>
    <#list groups as group>
      <#compress>${group.name}</#compress><#t/>
      <#if group_index &lt; groups?size - 1>,<#t/></#if>
    </#list>
  <#else>
    <@vrtx.msg code="permissions.not.assigned" default="Not assigned" /> <#t/>
  </#if>
</#macro>


<#--
 * editACLForm
 *
 * Macro for displaying the 'Edit ACL' form
 *
 * @param formName the - name of the form
 * @param privilegeName - the privilege to edit
 *
-->
  <#-- 
    Should we use this to access form without having to do @spring.bind all the time?
    assign form=.vars[formName] /
  -->

<#macro editACLFormNew formName privilegeName privilegeHeading>
  <#assign privilege = aclInfo.privileges[privilegeName] />
  <#assign pseudoPrincipals = aclInfo.privilegedPseudoPrincipals[privilegeName] />
  <#assign groupingPrincipal = aclInfo.groupingPrivilegePrincipalMap[privilegeName] />

  <#assign grouped = false />

  <@spring.bind formName + ".submitURL" /> 
  <form class="aclEdit" action="${spring.status.value?html}" method="post">
    <h3>${privilegeHeading}</h3>
    <ul class="everyoneOrSelectedUsers" id="${privilegeHeading}">
    <@spring.bind formName + ".grouped" /> 
    <#assign grouped = spring.status.value />
    <li>
      <input onclick="disableInput()"
             id="permissions.grouped"
             type="radio"
             name="${spring.status.expression}"
             value="true" <#if spring.status.value>checked="checked"</#if>> 
      <label id="permissions.grouped" for="permissions.grouped">
        <@vrtx.msg code="permissions.allowedFor.${groupingPrincipal.name}"
                   default="${groupingPrincipal.name}" /></label>
    </li>
    <li><input onclick="enableInput()"
               id="permissions.selectedPrincipals"
               type="radio"
               name="${spring.status.expression}"
               value="false"
               <#if !spring.status.value>checked="checked"</#if>> 
      <label id="permissions.selectedPrincipals" for="permissions.selectedPrincipals">
        <@vrtx.msg code="permissions.selectedPrincipals" default="Restricted to"/></label>
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
                             default="owner"/>&nbsp;(<@displayUserPrincipal principal=resourceContext.currentResource.owner />)</li>
              <#break>
            <#case "pseudo:authenticated">
            <#case "pseudo:all">
              <#if user.name != groupingPrincipal.name>
                <li><@vrtx.msg code="pseudoPrincipal." + user.name default="pseudoPrincipal." + user.name /><#t/>
                  <#if removeUserURLs?exists && removeUserURLs[user.name]?exists >
                    &nbsp;(&nbsp;<a href="${removeUserURLs[user.name]?html}"><#t/>
                      <#t/><@vrtx.msg code="permissions.remove" default="remove"/></a>&nbsp;)
                  </#if>
              </#if>
              <#break>
            <#default>
              <li><@displayUserPrincipal principal=user />
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
      <div class="errorContainer">
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
          <li>${error}</li> 
          </#list>
        </ul>
      </div>
      <#if spring.status.value?exists>
        <#assign value = spring.status.value />
      </#if>
      </#if>
      <span class="addUser">
        <@autocomplete.createAutoCompleteInputField appSrcBase="${autoCompleteBaseURL}" service="${spring.status.expression}" 
                    id="${spring.status.expression}" value="${value?html}" minChars="4" selectFirst="false" width="300" 
                    hasDescription=true max="30" />&nbsp;
        
        <@spring.bind formName + ".ac_userNames" />
        <input type="hidden" id="ac_userNames" name="ac_userNames" value="" />
        
	    <input class="addUserButton" type="submit" name="addUserAction"
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
          ${group.name}&nbsp;(&nbsp;<a href="${removeGroupURLs[group.name]?html}"><#t/>
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
        <#assign value=spring.status.value />
      </#if>
      </#if>
      <span class="addGroup">
	<input class="addGroupField" type="text" size="15"
               name="${spring.status.expression}"
               value="${value}" />&nbsp;
        <input class="addGroupButton" type="submit" name="addGroupAction"
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
</#macro>


<#macro displayUserPrincipal principal>
<#compress>
<#if principal.URL?exists>
  <a title="${principal.name?html}" href="${principal.URL?html}">${principal.description?html}</a>
<#else>
  ${principal.name?html}
</#if>
</#compress>
</#macro>
