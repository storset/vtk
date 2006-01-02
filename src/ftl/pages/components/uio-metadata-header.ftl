<#--
  - File: uio-metadata-header.ftl
  - 
  - Description: Header component for editing UiO visual profile properties
  - 
  - Required model data:
  -   resourceProperties
  -  
  - Optional model data:
  -   editResourcePropertyForm
  -
  -->

<#if !resourceProperties?exists>
  <#stop "Unable to render model: required submodel
  'resourceProperties' missing">
</#if>

<#import "/lib/vortikal.ftl" as vrtx />

  <div class="resourceInfoHeader" style="padding-bottom:1.5em;margin-top:0px;padding-top:0px;">
    <h2 class="permissionsToggleHeader">
      <@vrtx.msg code="visualProfile.recommended.header" default="Recommended UiO-metadata"/>
    </h2>
    <div class="permissionsToggleAction">
      <#if resourceContext.currentResource.getProperty("http://www.uio.no/visuell-profil/arv","arv")?exists>
        <@vrtx.msg code="visualProfile.selfdefined" default="Self-defined metadata"/>
      <#else>
        <@vrtx.msg code="visualProfile.predefined" default="Pre-defined metadata"/>
      </#if>

      <#list resourceProperties.propertyDescriptors as descriptor>
        <#if descriptor.name = 'arv' && descriptor.namespace = 'http://www.uio.no/visuell-profil/arv'>
          <#assign arv_index = descriptor_index>
          <#break>
        </#if>
      </#list>  

      <#if arv_index?exists>
        <#if resourceProperties.editPropertiesServiceURLs?exists &&
             resourceProperties.editPropertiesServiceURLs[arv_index]?exists>
          (&nbsp;<a href="${resourceProperties.editPropertiesServiceURLs[arv_index]?html}"><@vrtx.msg code="propertyEditor.edit" default="edit" /></a>&nbsp;)
        </#if>
      </#if>
    </div>

    <@prop.listproperties descriptors=resourceProperties.propertyDescriptors namespace="http://www.uio.no/visuell-profil/arv" outsideTable=true nullValue=true radio=true />
  </div>
