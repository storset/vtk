<#ftl strip_whitespace=true>

<#--
  - File: properties-listing.ftl
  - 
  - Description: A HTML page that displays resource properties
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#macro propertyList propertyList>
  <div style="padding-left:0.5em;padding-right:0.5em;padding-bottom:1em;">

    <table style="clear: both;" class="resourceInfo">
      <#list propertyList as item>
        <tr>
          <#if form?exists && form.definition?exists && form.definition = item.definition>
            <td colspan="3" class="expandedForm">
              <form action="${form.submitURL?html}" method="POST">
                <h3>${item.definition.name}:</h3>
                <ul class="property">
                  <#if form.possibleValues?exists>
                    <#list form.possibleValues as alternative>
                      <#if alternative?has_content>
                        <li><input id="${alternative}" type="radio" name="value" value="${alternative}"
                                   <#if form.value?has_content && form.value = alternative>checked</#if>>
                          <label for="${alternative}">${alternative}</label></li>
                        <#else>
                          <li><input id="unset" type="radio" name="value" value=""
                                     <#if !form.value?has_content>checked</#if>>
                            <label for="unset">Not set</label></li>
                        </#if>
                      </#list>
                    <#else>
                      <#if form.value?exists>
                         <#if item.definition.type = 5>
                	        <#assign value=item.property.principalValue.name>
                         <#else>
                            <#assign value=item.property.value>
                         </#if>
                      </#if>
                      <li><input type="text" name="value" value="${value}">
                        </li>
                    </#if>

                </ul>
                <@spring.bind "form.value"/>

                <#if spring.status.errorCodes?size &gt; 0>
                  <ul class="errors">
                    <#list spring.status.errorCodes as error> 
                      <li>${error}</li> 
                    </#list>
	          </ul>
                </#if>

                <input type="submit" name="save"
                       value="<@vrtx.msg code="propertyEditor.save" default="Save"/>">
                <input type="submit" name="cancelAction"
                       value="<@vrtx.msg code="propertyEditor.cancel" default="Cancel"/>">
              </form>
            </td>
          <#else>
            <td>${item.definition.name}</td>
            <td>
              <#if item.property?exists>
                <#if item.definition.multiple>
                    <#list item.property.values as val>
                      ${val?string}<#if val_has_next>, </#if>
                    </#list>
                  <#if item.property.values?size &lt; 0>
                  </#if>
                <#else>
                  <#-- type principal = 5 -->
                  <#if item.definition.type = 5>
                	 ${item.property.principalValue.name}
                  <#else>
                     ${(item.property.value?string)}
                  </#if>
                </#if>
              <#else>
                  Not set
              </#if>
            </td>
            <#-- type boolean = 4 -->
           <td><#if item.editURL?exists>( <a href="${item.editURL?html}"><#if item.definition.type = 4><@vrtx.msg code="propertyEditor.toggle" default="toggle" /><#else><@vrtx.msg code="propertyEditor.edit" default="edit" /></#if></a> )</#if></td>
          </#if>
        </tr>
      </#list>
    </table>

  </div>
</#macro>
