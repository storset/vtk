<#ftl strip_whitespace=true>
<#--
  - File: kupu-list-libraries.ftl
  - 
  - Description: Generates a list of libraries to browse for links and images.
  - 
  - Required model data:
  -   resourceContext
  -   currentFolderLibrary
  -   allFilesAndFoldersLibrary
  -  
  - Optional model data:
  -
  -->
<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>
<#if !currentFolderLibrary?exists>
  <#stop "Unable to render model: required submodel
  'currentFolderLibrary' missing">
</#if>
<#if !allFilesAndFoldersLibrary?exists>
  <#stop "Unable to render model: required submodel
  'allFilesAndFoldersLibrary' missing">
</#if>
<?xml version="1.0" encoding="UTF-8"?>

<libraries>
  <library id="library:${resourceContext.currentResource.URI?html}">
    <title>Current folder</title>
    <!--icon>foobar.png</icon-->
    <src>${currentFolderLibrary.url?html}</src>
  </library>

  <#if resourceContext.currentResource.URI != '/'>
  <library id="library:/">
    <title>All files and folders</title>
    <!--icon>foobar.png</icon-->
    <src>${allFilesAndFoldersLibrary.url?html}</src>
  </library>
  </#if>
</libraries>
