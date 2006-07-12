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
  <div>
    <table class="resourceInfo">
      <#list propertyList as item>
        <@editOrDisplayPropertyItem item />
      </#list>
    </table>
  </div>
</#macro>


<#macro editOrDisplayPropertyItem item propertyDisplayFormat=defaultPropertyDisplayFormat>
  <#if form?exists && form.definition?exists && form.definition = item.definition>
    <tr>
      <td colspan="2" class="expandedForm">
        <@propertyForm item />
      </td>
    </tr>
  <#else>
    <@propertyDisplay item=item />
  </#if>
</#macro>


<#assign defaultPropertyDisplayFormat>
  <#noparse>
  <tr>
    <td class="key">
      ${name}:
    </td>
    <td>
      ${value}
      ${editURL}
    </td>
  </tr>
  </#noparse>
</#assign>


<#--
 * propertyDisplay
 *
 * Display a org.vortikal.web.controller.properties.PropertyItem,
 * usually in this format: [propertyName]: [propertyValue] (editURL)
 * Example: <@propertyDisplay item=myItem />
 *
 * @param item a org.vortikal.web.controller.properties.PropertyItem
 *        representing a resource property
 * @param propertyDisplayFormat (optional) a string containing 
 *        Freemarker code for displaying the name, value and editURL
 *        of a proprty. The variables "name", "value" and "editURL"
 *        are supplied and available for interpolation. The default  
 *        display format is defined in the variable
 *        "defaultPropertyDisplayFormat".
-->
<#macro propertyDisplay item propertyDisplayFormat=defaultPropertyDisplayFormat>
  <#local localizedValueLookupKeyPrefix>
    <#compress>
      <#if item.definition.namespace.uri?exists>
        property.${item.definition.namespace.uri}:${item.definition.name}
      <#else>
        property.${item.definition.name}
      </#if>
    </#compress>
  </#local>
  <#local name = vrtx.getMsg(localizedValueLookupKeyPrefix, item.definition.name) />
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
          <#local label>
            <@vrtx.msg code="${localizedValueLookupKeyPrefix}.${item.property.value?string}"
                       default="${item.property.value?string}" />
          </#local>
          ${label}
        </#if>
      </#if>
    <#else>
      <#local defaultNotSet><@vrtx.msg code="property.unset" default="Not set" /></#local>
      <#local label>
        <@vrtx.msg code="${localizedValueLookupKeyPrefix}.unset"
                       default="${defaultNotSet}" />
      </#local>
      ${label}
    </#if>
  </#local>
  <#local editURL>
    <@propertyEditURL item = item />
  </#local>
  <#local display = propertyDisplayFormat?interpret />
  <@display />
</#macro>



<#--
 * propertyForm
 *
 * Display a form for editing a property contained in a
 * org.vortikal.web.controller.properties.PropertyItem object,
 *
 * @param item a org.vortikal.web.controller.properties.PropertyItem
 *        representing a resource property
 * @param formValue (optional) the value to insert in the input field
 *        (only applies to properties that are not limited to a number
 *        of fixed values). This value, if specified, overrides the one
 *        usually obtained from the property itself.
-->
<#macro propertyForm item formValue="">
  <#local localizedValueLookupKeyPrefix>
    <#compress>
      <#if item.definition.namespace.uri?exists>
        property.${item.definition.namespace.uri}:${item.definition.name}
        <#else>
          property.${item.definition.name}
        </#if>
      </#compress>
    </#local>
    <form action="${form.submitURL?html}" method="POST">
      <@formWrapper item>
        <#-- Display radio buttons for a value set of 2: -->

        <#if form.possibleValues?exists && form.possibleValues?size = 2>
          <#list form.possibleValues as alternative>
            <#if alternative?has_content>
              <@formInputWrapper item>
              <#local label><@vrtx.msg code="${localizedValueLookupKeyPrefix}.${alternative}" default="${alternative}" /></#local>
              <input id="${alternative}" type="radio" name="value" value="${alternative}"
                         <#if form.value?has_content && form.value = alternative>checked</#if>>
                <label for="${alternative}">${label}</label>
              </@formInputWrapper>
            <#else>
              <@formInputWrapper item>
              <#local defaultNotSet><@vrtx.msg code="resource.property.unset" default="Not set" /></#local>
              <#local label><@vrtx.msg code="${localizedValueLookupKeyPrefix}.unset" default="${defaultNotSet}" /></#local>
                <input id="unset" type="radio" name="value" value=""
                           <#if !form.value?has_content>checked</#if>>
                  <label for="unset">${label}</label>
              </@formInputWrapper>
            </#if>
          </#list>

        <#-- Display drop down list for value sets > 2: -->

        <#elseif form.possibleValues?exists && form.possibleValues?size &gt; 2>
          <@formInputWrapper item>
          <select name="value">
          <#list form.possibleValues as alternative>
            <#if alternative?has_content>
              <#local label>
                <@vrtx.msg code="${localizedValueLookupKeyPrefix}.${alternative}"
                           default="${alternative}" />
              </#local>
              <option id="${alternative}" 
                      <#if form.value?has_content && form.value = alternative>selected="true"</#if>
                      label="${label}">${label}</option>
            <#else>
              <#local defaultNotSet><@vrtx.msg code="resource.property.unset" default="Not set" /></#local>
              <#local label><@vrtx.msg code="${localizedValueLookupKeyPrefix}.unset" default="${defaultNotSet}" /></#local>
              <option id="unset" value=""
                 <#if !form.value?has_content>selected="true"</#if>
                 label="${label}">${label}</option>
            </#if>
          </#list>
          </select>
          </@formInputWrapper>

        <#-- Display regular input field for plain values: -->

        <#else>
          <#local value>
            <#compress>
            <#if formValue != "">
              ${formValue}
            <#elseif form.value?exists>
              ${form.value}
            <#else>
            </#if>
            </#compress>
          </#local>
          <@formInputWrapper item>
            <input type="text" name="value" value="${value}">
            <#if item.format?exists>(${item.format})</#if>
          </@formInputWrapper>

        </#if>
      <@spring.bind "form.value"/>
      <#if spring.status.errorCodes?size &gt; 0>
        <@formErrorsWrapper>
          <#list spring.status.errorCodes as error> 
            <@formErrorWrapper>${error}</@formErrorWrapper>
          </#list>
        </@formErrorsWrapper>
      </#if>

      <input type="submit" name="save"
             value="<@vrtx.msg code="propertyEditor.save" default="Save"/>">
      <input type="submit" name="cancelAction"
             value="<@vrtx.msg code="propertyEditor.cancel" default="Cancel"/>">
      </@formWrapper>
    </form>
</#macro>


<#macro formWrapper item>
  <#local localizedValueLookupKeyPrefix>
    <#compress>
      <#if item.definition.namespace.uri?exists>
        property.${item.definition.namespace.uri}:${item.definition.name}
      <#else>
        property.${item.definition.name}
      </#if>
    </#compress>
  </#local>
  <#local name = vrtx.getMsg(localizedValueLookupKeyPrefix, item.definition.name) />
  <h3>${name}:</h3>
  <ul class="property">
    <#nested />
  </ul>
</#macro>

<#macro formInputWrapper item>
  <li><#nested /></li>
</#macro>

<#macro formErrorsWrapper>
  <ul><#nested /></ul>
</#macro>

<#macro formErrorWrapper>
  <li><#nested /></li>
</#macro>

<#macro propertyEditURL item>
  <#if item.editURL?exists>
     ( <a href="${item.editURL?html}"><@vrtx.msg code="propertyEditor.edit" default="edit" /></a> )
  </#if>
</#macro>
