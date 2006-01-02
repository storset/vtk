<#ftl strip_whitespace=true>

<#--
  - File: display-breadcrumb.ftl
  - 
  - Description: Breadcrumb view component
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/lib/breadcrumb.ftl" as brdcrmb />

<#if !breadcrumb?exists>
  <#stop "Unable to render model: required submodel
  'breadcrumb' missing">
</#if>

<@brdcrmb.breadCrumb crumbs=breadcrumb />
