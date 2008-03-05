<#--
  - File: breadcrumb.ftl
  - 
  - Description: header that displays a breadcrumb and a resource title
  - 
  - Required model data:
  -   breadcrumb
  -
  - Optional model data:
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>
<#import "/lib/breadcrumb.ftl" as brdcrmb/>

<#if !breadcrumb?exists>
  <#stop "Unable to render model: required submodel
  'breadcrumb' missing">
</#if>

<#assign downcaseElements = false />
<#if (config.downcase)?exists && (config.downcase) = 'true'>
  <#assign downcaseElements = true />
</#if>

<@brdcrmb.breadCrumb crumbs=breadcrumb downcase=downcaseElements />
