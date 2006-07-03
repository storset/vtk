<#ftl strip_whitespace=true>

<#--
  - File: properties-listing.ftl
  - 
  - Description: A HTML page that displays resource properties
  - 
  - Required model data:
  -     - aboutItems
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#macro propertyList propertyList>
  <div style="padding-left:0.5em;padding-right:0.5em;padding-bottom:1em;">

    <table style="clear: both;" class="resourceInfo">
      <#list propertyList as item>
        <@editOrDisplay item />
      </#list>
    </table>
  </div>
</#macro>


<#macro editOrDisplayPropertyItem item>
  <tr>
    <#if form?exists && form.definition?exists && form.definition = item.definition>
      <@propertyForm item />
    <#else>
      <@propertyDisplay item />
    </#if>
  </tr>
</#macro>


<#macro propertyDisplay item>
  <#local key = vrtx.getMsg("resource." + item.definition.name, item.definition.name) />
  <#local value>
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
      <@vrtx.msg code="resource.property.unspecified" default="Not set" />
    </#if>
  </#local>
  <#local editURL>
    <@propertyEditURL item = item />
  </#local>

  <@defaultPropertyDisplay key=key value=value editURL=editURL />
</#macro>


<#macro defaultPropertyDisplay key value editURL="">
  <td class="key">
    ${key}:
  </td>
  <td>
    ${value}
    ${editURL}
  </td>
</#macro>

<#macro propertyForm item formValue="" >
  <td colspan="3" class="expandedForm">
    <form action="${form.submitURL?html}" method="POST">
      <#local name = vrtx.getMsg("resource." + item.definition.name, item.definition.name) />
      <h3>${name}:</h3>
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
          <#local value="${formValue}" />
          <#if form.value?exists && item.property?exists>
            <#if item.definition.type = 5>
              <#local value=item.property.principalValue.name>
            <#else>
              <#local value=item.property.value>
            </#if> 
          </#if>
          <li>
            <input type="text" name="value" value="${value?if_exists}">
            <#if item.format?exists>(${item.format})</#if>
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
</#macro>


<#macro propertyEditURL item>
  <#if item.editURL?exists>
     ( <a href="${item.editURL?html}">
      <#if item.definition.type = 4> <#-- type boolean = 4 -->
        <@vrtx.msg code="propertyEditor.toggle" default="toggle" />
      <#else>
        <@vrtx.msg code="propertyEditor.edit" default="edit" />
      </#if>
    </a> )
  </#if>
</#macro>
