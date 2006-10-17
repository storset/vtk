<#ftl strip_whitespace=true>

<#--
  - File: properties-macro.ftl
  - 
  - Description: A macro for editing resource properties.
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  - TODO: Should be made more generic and placed in 'lib'.
  -
  -->


<#--
 * listproperties
 *
 * Macro for editing properties (needs to be more generic) 
 *
 * @param descriptors list of propertyDescriptor objects
 * @param namespace common namespace for the properties to edit
 * @param outsideTable (true/false) for use with variables displayed
 *        outside property-table
 * @param nullValue (true/false) true if a property could be set to a
 *        unset-value (and by that deleted from the resource)
 * @param radio (true/false) true if editing the properties should use
 *        radiobuttons instead of select/input-form widgets
 *
-->
<#macro listproperties descriptors namespace outsideTable=false nullValue=false radio=false>
  <#list descriptors as descriptor>
    <#if editResourcePropertyForm?exists &&
         editResourcePropertyForm.namespace = descriptor.namespace &&
         descriptor.namespace = namespace &&
         editResourcePropertyForm.name = descriptor.name &&
         !editResourcePropertyForm.done >
      <#if outsideTable><table class="resourceInfo" style="clear:both"></#if>
      <tr class="property">
      <td colspan="2" class="expandedForm">
        <form action="${editResourcePropertyForm.submitURL?html}" method="POST">
          <h3><@vrtx.msg code="property.${descriptor.namespace}:${descriptor.name}.description"
                         default="${descriptor.name}"/>:</h3>
          <ul class="property" style="padding-left:0;margin-left:0;">
            <#-- If no possibleValues, give user just a simple input form -->
            <#if !editResourcePropertyForm.possibleValues?has_content>
              <li><input type="text" name="value" size="30" <#if editResourcePropertyForm.value?has_content>value="${editResourcePropertyForm.value}"</#if>/></li>
            <#else>
            <#if !radio><li><select name="value"></#if>
            <#list editResourcePropertyForm.possibleValues as alternative>
              <#if alternative?has_content>
                <#assign msgCode = "property." + editResourcePropertyForm.namespace + ":" +
                     editResourcePropertyForm.name + ".value." + alternative />
                <#if !radio><option id="${alternative}" value="${alternative}"
                <#else><li><input id="${alternative}" type="radio" name="value" value="${alternative}"</#if>
                <#if editResourcePropertyForm.value?has_content
                     && editResourcePropertyForm.value = alternative><#if !radio>selected<#else>checked</#if></#if>>
                    <#if !radio><@vrtx.msg code="${msgCode}" default="${alternative}"/></option>
                    <#else><label for="${alternative}"><@vrtx.msg code="${msgCode}" default="${alternative}"/></label></li></#if>
              <#elseif nullValue>
                <#assign msgCode = "property." + editResourcePropertyForm.namespace + ":" +
                         editResourcePropertyForm.name + ".unset" />
                <#if !radio><option id="unset" value=""<#else><li><input id="unset" type="radio" name="value" value=""</#if>
                <#if !editResourcePropertyForm.value?has_content><#if !radio>selected<#else>checked</#if></#if>>
                <#if !radio><@vrtx.msg code="${msgCode}" default="Not set"/></option><#else><label for="unset"><@vrtx.msg code="${msgCode}" default="Not set"/></label></li></#if>          
              </#if>
            </#list>
            <#if !radio></select></li></#if>
          </#if>
          </ul>
          <input type="submit" name="save" value="<@vrtx.msg code="propertyEditor.save" default="Save"/>">
          <input type="submit" name="cancelAction" value="<@vrtx.msg code="propertyEditor.cancel" default="Cancel"/>">
        </form>
      </td>
      </tr>                                                                      
      <#if outsideTable></table></#if>
    <#else>
      <#if descriptor.namespace = namespace && !outsideTable>
      <tr class="property">
      <td class="propertyName key">
        <@vrtx.msg code="property.${descriptor.namespace}:${descriptor.name}.description"
                   default="${descriptor.name}"/>:
      </td>
      <td class="propertyValue value">
        <#if resourceProperties.propertyValues[descriptor_index]?exists>
          <#if resourceProperties.propertyValues[descriptor_index]?has_content>
            <@vrtx.msg code="property.${descriptor.namespace}:${descriptor.name}.value.${resourceProperties.propertyValues[descriptor_index]}"
                       default="${resourceProperties.propertyValues[descriptor_index]}"/>
          <#else>
              <@vrtx.msg code="property.${descriptor.namespace}:${descriptor.name}.unset"
                         default=""/>
          </#if>
        <#else>
          <@vrtx.msg code="property.${descriptor.namespace}:${descriptor.name}.unset"
                     default=""/>
        </#if>
        <#if resourceProperties.editPropertiesServiceURLs?exists &&
             resourceProperties.editPropertiesServiceURLs[descriptor_index]?exists>
          (&nbsp;<a href="${resourceProperties.editPropertiesServiceURLs[descriptor_index]?html}"><@vrtx.msg code="propertyEditor.edit" default="edit" /></a>&nbsp;)
        </#if>
      </td>
      </tr>
      </#if>
    </#if>
   </#list>
</#macro>


