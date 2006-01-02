<#--
  - File: resource-properties.ftl
  - 
  - Description: Component that displays the custom properties on a
  - resource in a table
  - 
  - Required model data:
  -   resourceProperties
  -  
  - Optional model data:
  -   editResourcePropertyForm
  -
  -->
<#if !resourceProperties?exists>
  <#stop "Unable to render model: required model data
  'resourceProperties' missing">
</#if>

<#import "/lib/vortikal.ftl" as vrtx />

	
<#if resourceProperties.propertyDescriptors?has_content>

  <#list resourceProperties.propertyDescriptors as descriptor>
    <#if descriptor.name="visual-profile"  && descriptor_index = 0> 
      <h2 class="resourceInfoHeader" style="padding-top:15px;">
        <@vrtx.msg code="resourceProperties.visualProfile" default="UiOs visuelle profil"/>
      </h2>
      <table class="resourceInfo">
    <#elseif descriptor_index = 0>
      <h2 class="resourceInfoHeader">
        <@vrtx.msg code="resourceProperties.header.${resourceContext.currentResource.contentType}" default="Properties set on this resource"/>
      </h2>
      <table class="resourceInfo">
    </#if>

    <tr class="property <#if descriptor.name!="visual-profile">grey</#if>">
    <#if editResourcePropertyForm?exists &&
         editResourcePropertyForm.namespace = descriptor.namespace &&
         editResourcePropertyForm.name = descriptor.name &&
         !editResourcePropertyForm.done >
      <td colspan="2" class="expandedForm">
        <form action="${editResourcePropertyForm.submitURL?html}" method="POST">
          <h3><@vrtx.msg code="property.${descriptor.namespace}:${descriptor.name}.description"
                         default="${descriptor.name}"/>:</h3>
          <ul class="property">
          <#list editResourcePropertyForm.possibleValues as alternative>
            <#if alternative?has_content>
            <#assign msgCode = "property." + editResourcePropertyForm.namespace + ":" +
                     editResourcePropertyForm.name + ".value." + alternative />
            <li><input id="${alternative}" type="radio" name="value" value="${alternative}"
                       <#if editResourcePropertyForm.value?has_content
                       && editResourcePropertyForm.value = alternative>checked</#if>>
              <label for="${alternative}"><@vrtx.msg code="${msgCode}" default="${alternative}"/></label></li>
            <#else>
              <#assign msgCode = "property." + editResourcePropertyForm.namespace + ":" +
                     editResourcePropertyForm.name + ".unset" />
              <li><input id="unset" type="radio" name="value" value=""
                       <#if !editResourcePropertyForm.value?has_content>checked</#if>>
              <label for="unset"><@vrtx.msg code="${msgCode}" default="Not set"/></label></li>
            </#if>
          </#list>
          </ul>
          <input type="submit" name="save" value="<@vrtx.msg code="propertyEditor.save" default="Save"/>">
          <input type="submit" name="cancelAction" value="<@vrtx.msg code="propertyEditor.cancel" default="Cancel"/>">
        </form>
      </td>
    <#else>
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
                         default="Not set"/>
          </#if>
        <#else>
          <@vrtx.msg code="property.${descriptor.namespace}:${descriptor.name}.unset"
                     default="Not set"/>
        </#if>
        <#if resourceProperties.editPropertiesServiceURLs?exists &&
             resourceProperties.editPropertiesServiceURLs[descriptor_index]?exists>
          (&nbsp;<a href="${resourceProperties.editPropertiesServiceURLs[descriptor_index]?html}"><@vrtx.msg code="propertyEditor.edit" default="edit" /></a>&nbsp;)
        </#if>
      </td>
    </#if>
    </tr>

    <#if descriptor.name="visual-profile" && descriptor_has_next> 
    </table>    
    <h2 class="resourceInfoHeader">
      <@vrtx.msg code="resourceProperties.header.${resourceContext.currentResource.contentType}" default="Properties set on this resource"/>
    </h2>
    <table class="resourceInfo">
    </#if>
  </#list>
</table>
</#if>
