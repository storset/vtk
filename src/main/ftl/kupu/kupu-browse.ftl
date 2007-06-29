<#ftl strip_whitespace=true>
<#--
  - File: kupu-browse.ftl
  - 
  - Description: XML collection browsing for kupu
  - 
  - Required model data:
  -   resourceContext
  -   collectionListing
  -   viewService
  -   cssBaseURL
  -  
  - Optional model data:
  -   parentURL
  -   viewparentService
  -
  -->
<#if !collectionListing?exists>
  <#stop "Unable to render model: required submodel
  'collectionListing' missing">
</#if>
<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>
<?xml version="1.0" encoding="UTF-8"?>

<collection id="resource:${resourceContext.currentResource.URI}">
  <title>${resourceContext.currentResource.name}</title>
  <#if viewService.url?exists>
  <uri>${viewService.url?html}</uri>
  </#if>
  <!--icon>foobar.png</icon-->

  <items>

    <#if resourceContext.currentResource.parent?exists>
      <collection id="resource:${resourceContext.currentResource.parent}">
        <title>Parent</title>
        <#if (parentURL.url)?has_content>
        <src>${parentURL.url?html}</src>
        </#if>
        <#if (viewParentService.url)?has_content>
        <uri>${viewParentService.url?html}</uri>
        </#if>
        <icon>${cssBaseURL}/up.gif</icon>
      </collection>
    </#if>
    
    <#if resourceContext.currentResource.name?exists>
      <resource id="resource:${resourceContext.currentResource.name}">
        <title>&lt;Current folder&gt;</title>
        <#if (viewService.url)?has_content>
        <uri>${viewService.url?html}</uri>
        </#if>
        <icon>${cssBaseURL}/dir2.gif</icon>
      </resource>
    </#if>

  <#list collectionListing.children as child>

    <#if collectionListing.browsingLinks[child_index]?exists>
      <collection id="resource:${child.URI}">
        <title>${child.name}</title>
        <#if collectionListing.childLinks[child_index]['view']?has_content>
        <uri>${collectionListing.childLinks[child_index]['view']?html}</uri>
        </#if>
        <src>${collectionListing.browsingLinks[child_index]?html}</src>
        <icon>${cssBaseURL}/dir2.gif</icon>
      </collection>
    <#else>
      <resource id="resource:${child.URI}">
        <title>${child.name}</title>
        <#if collectionListing.childLinks[child_index]['view']?has_content>
        <uri>${collectionListing.childLinks[child_index]['view']?html}</uri>
        </#if>
        <#if child.contentType?exists && child.contentType?starts_with("image")>
          <icon>${cssBaseURL}/image2.png</icon>
        <#else>
          <icon>${cssBaseURL}/file2.gif</icon>
        </#if>
      </resource>
    </#if>
  </#list>
  </items>
</collection>
