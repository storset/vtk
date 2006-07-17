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


<#macro propertyList modelName itemNames
        propertyListWrapperMacro='defaultPropertyListWrapper'
        displayMacro='defaultPropertyDisplay'
        editWrapperMacro='defaultEditWrapper'
        formWrapperMacro='defaultFormWrapper'
        formInputWrapperMacro='defaultFormInputWrapper'
        formSubmitWrapperMacro='defaultFormSubmitWrapper'
        formErrorsWrapperMacro='defaultFormErrorsWrapper'
        formErrorWrapperMacro='defaultFormErrorWrapper'>
  <#local itemList = [] />
  <#list itemNames as propertyName>
    <#if .vars[modelName][propertyName]?exists>
      <#local itemList = itemList + [ .vars[modelName][propertyName] ] /> 
    </#if>
  </#list>
  <@propertyItemList
     propertyList=itemList
     propertyListWrapperMacro=propertyListWrapperMacro 
     propertyListWrapperMacro=propertyListWrapperMacro
     displayMacro=displayMacro
     editWrapperMacro=editWrapperMacro
     formWrapperMacro=formWrapperMacro
     formInputWrapperMacro=formInputWrapperMacro
     formSubmitWrapperMacro=formSubmitWrapperMacro
     formErrorsWrapperMacro=formErrorsWrapperMacro
     formErrorWrapperMacro=formErrorWrapperMacro/>
</#macro>


<#macro propertyItemList propertyList 
        propertyListWrapperMacro='defaultPropertyListWrapper'
        displayMacro='defaultPropertyDisplay'
        editWrapperMacro='defaultEditWrapper'
        formWrapperMacro='defaultFormWrapper'
        formInputWrapperMacro='defaultFormInputWrapper'
        formSubmitWrapperMacro='defaultFormSubmitWrapper'
        formErrorsWrapperMacro='defaultFormErrorsWrapper'
        formErrorWrapperMacro='defaultFormErrorWrapper'>
  <#local wrapperMacro = resolveMacro(propertyListWrapperMacro) />
  <@wrapperMacro>
    <#list propertyList as item>
      <@editOrDisplayPropertyItem item />
    </#list>
  </@wrapperMacro>
</#macro>


<#macro editOrDisplayProperty modelName propertyName
        propertyListWrapperMacro='defaultPropertyListWrapper'
        displayMacro='defaultPropertyDisplay'
        editWrapperMacro='defaultEditWrapper'
        formWrapperMacro='defaultFormWrapper'
        formInputWrapperMacro='defaultFormInputWrapper'
        formSubmitWrapperMacro='defaultFormSubmitWrapper'
        formErrorsWrapperMacro='defaultFormErrorsWrapper'
        formErrorWrapperMacro='defaultFormErrorWrapper'>
  <#if .vars[modelName]?exists && .vars[modelName][propertyName]?exists>
  <#local item = .vars[modelName][propertyName] />
  <#if form?exists && form.definition?exists && form.definition = item.definition>
    <@propertyForm 
       item=item
       editWrapperMacro=editWrapperMacro
       formWrapperMacro=formWrapperMacro
       formInputWrapperMacro=formInputWrapperMacro
       formSubmitWrapperMacro=formSubmitWrapperMacro
       formErrorsWrapperMacro=formErrorsWrapperMacro
       formErrorWrapperMacro=formErrorWrapperMacro/>
  <#else>
    <@propertyDisplay item=item displayMacro=displayMacro/>
  </#if>
  </#if>
</#macro>


<#macro editOrDisplayPropertyItem item
        propertyListWrapperMacro='defaultPropertyListWrapper'
        displayMacro='defaultPropertyDisplay'
        editWrapperMacro='defaultEditWrapper'
        formWrapperMacro='defaultFormWrapper'
        formInputWrapperMacro='defaultFormInputWrapper'
        formSubmitWrapperMacro='defaultFormSubmitWrapper'
        formErrorsWrapperMacro='defaultFormErrorsWrapper'
        formErrorWrapperMacro='defaultFormErrorWrapper'>
  <#if form?exists && form.definition?exists && form.definition = item.definition>
    <@propertyForm 
       item=item
       editWrapperMacro=editWrapperMacro
       formWrapperMacro=formWrapperMacro
       formInputWrapperMacro=formInputWrapperMacro
       formSubmitWrapperMacro=formSubmitWrapperMacro
       formErrorsWrapperMacro=formErrorsWrapperMacro
       formErrorWrapperMacro=formErrorWrapperMacro/>
  <#else>
    <@propertyDisplay item=item displayMacro=displayMacro/>
  </#if>
</#macro>


<#macro defaultPropertyListWrapper>
  <table class="resourceInfo">
    <#nested />
  </table>
</#macro>

<#macro defaultPropertyDisplay name value editURL="">
  <tr>
    <td class="key">
      ${name}:
    </td>
    <td>
      ${value}
      <#if editURL != "">
        ${editURL}
      </#if>
    </td>
  </tr>
</#macro>

<#macro defaultEditWrapper>
  <tr>
    <td colspan="2" class="expandedForm">
      <#nested /> 
    </td>
  </tr>
</#macro>

<#macro defaultFormWrapper item>
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

<#macro defaultFormInputWrapper item>
  <li><#nested /></li>
</#macro>

<#macro defaultFormSubmitWrapper item>
  <li><div><#nested /></div></li>
</#macro>

<#macro defaultFormErrorsWrapper>
  <ul class="errors"><#nested /></ul>
</#macro>

<#macro defaultFormErrorWrapper>
  <li><#nested /></li>
</#macro>



<#--
 * propertyDisplay
 *
 * Display a org.vortikal.web.controller.properties.PropertyItem,
 * usually in this format: [propertyName]: [propertyValue] (editURL)
 * Example: <@propertyDisplay item=myItem />
 *
 * @param item a org.vortikal.web.controller.properties.PropertyItem
 *        representing a resource property
 * @param displayMacro a macro that takes parameters "name", "value"
 *        and "editURL", used for displaying the property.
 *        Default is 'defaultPropertyDisplay'
-->
<#macro propertyDisplay item displayMacro='defaultPropertyDisplay'>
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
        <#elseif item.definition.type = 3>
          ${item.property.dateValue?date}
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
    <@propertyItemEditURL item = item />
  </#local>
  
  <#local macroCall = resolveMacro(displayMacro) />
  <@macroCall name=name value=value editURL=editURL />
</#macro>




<#--
 * propertyForm
 *
 * Display a form for editing a property contained in a
 * org.vortikal.web.controller.properties.PropertyItem object,
 *
 * @param item a org.vortikal.web.controller.properties.PropertyItem
 *        representing a resource property
 *        usually obtained from the property itself.
 * @param editWrapperMacro a macro for wrapping around the <form> element
 * @param formWrapperMacro a macro for wrapping inside the <form> element
 * @param formInputWrapperMacro a macro for wrapping a <input> element
 * @param formSubmitWrapperMacro a macro for wrapping the submit elements
 * @param formErrorsWrapperMacro a macro for wrapping the error list
 * @param formErrorWrapperMacro a macro for wrapping a form error
-->
<#macro propertyForm item formValue=''
        editWrapperMacro='defaultEditWrapper'
        formWrapperMacro='defaultFormWrapper'
        formInputWrapperMacro='defaultFormInputWrapper'
        formSubmitWrapperMacro='defaultFormSubmitWrapper'
        formErrorsWrapperMacro='defaultFormErrorsWrapper'
        formErrorWrapperMacro='defaultFormErrorWrapper'>

  <#local localizedValueLookupKeyPrefix>
    <#compress>
      <#if item.definition.namespace.uri?exists>
        property.${item.definition.namespace.uri}:${item.definition.name}
        <#else>
          property.${item.definition.name}
        </#if>
      </#compress>
    </#local>

    <#local editWrapper = resolveMacro(editWrapperMacro) />
    <#local formWrapper = resolveMacro(formWrapperMacro) />
    <#local formInputWrapper = resolveMacro(formInputWrapperMacro) />
    <#local formSubmitWrapper = resolveMacro(formSubmitWrapperMacro) />
    <#local formErrorsWrapper = resolveMacro(formErrorsWrapperMacro) />
    <#local formErrorWrapper = resolveMacro(formErrorWrapperMacro) />

    <@editWrapper>
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

      <@formSubmitWrapper item>
      <input type="submit" name="save"
             value="<@vrtx.msg code="propertyEditor.save" default="Save"/>">
      <input type="submit" name="cancelAction"
             value="<@vrtx.msg code="propertyEditor.cancel" default="Cancel"/>">
      </@formSubmitWrapper>
      </@formWrapper>
    </form>
    </@editWrapper>
</#macro>


<#macro propertyEditURL modelName propertyName>
  <#if .vars[modelName][propertyName]?exists>
      <#local item =  .vars[modelName][propertyName] /> 
      <#if item.editURL?exists>
        ( <a href="${item.editURL?html}"><@vrtx.msg code="propertyEditor.edit" default="edit" /></a> )
      </#if>
  </#if>
</#macro>

<#macro propertyItemEditURL item>
  <#if item.editURL?exists>
     ( <a href="${item.editURL?html}"><@vrtx.msg code="propertyEditor.edit" default="edit" /></a> )
  </#if>
</#macro>


<#function resolveMacro macroName>
  <#local macroCall = "" />
  <#if macroName?eval?exists && macroName?eval?is_macro>
    <#local macroCall = macroName?eval />
  <#elseif .main[macroName]?exists && .main[macroName]?is_macro>
    <#local macroCall = .main[macroName] />
  <#else>
    <#stop "No such macro: ${macroName}" />
  </#if>        
  <#return macroCall />
</#function>
