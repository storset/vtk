<#ftl strip_whitespace=true>
<#--
  - File: kupu-library.ftl
  - 
  - Description: XML library browsing for kupu
  - 
  - Required model data:
  -   collectionListing
  -   resourceContext
  -   viewService
  -   cssBaseURL
  -  
  - Optional model data:
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

<library id="library:${resourceContext.currentResource.URI}">
  <title>${resourceContext.currentResource.name}</title>
  <#if viewService.url?exists>
  <uri>${viewService.url?html}</uri>
  </#if>
  <!--icon>foobar.png</icon-->

  <items>

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
      <resource id="${child.URI}">
        <title>${child.name}</title>
        <#if collectionListing.childLinks[child_index]['view']?has_content>
        <uri>${collectionListing.childLinks[child_index]['view']?html}</uri>
        </#if>
        <#if child.contentType?starts_with("image")>
          <icon>${cssBaseURL}/image2.png</icon>
        <#else>
          <icon>${cssBaseURL}/file2.gif</icon>
        </#if>
      </resource>
    </#if>
  </#list>
  </items>
</library>
