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
<#if .vars['downcase']?exists && .vars['downcase'] = 'true'>
  <#assign downcaseElements = true />
</#if>

<#assign hide = false />
<#if .vars['hide-prefix']?exists && .vars['hide-prefix'] = 'true'>
  <#assign hide = true />
</#if>

<#assign stop = 0 />
<#if .vars['stop-at-level']?exists>
  <#assign stop = .vars['stop-at-level']?number />
</#if>

<@brdcrmb.breadCrumb crumbs=breadcrumb downcase=downcaseElements hidePrefix=hide stopLevel=stop />
