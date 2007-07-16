<#--
  - File: breadcrumb.ftl
  - 
  - Description: Macro generating a breadcrumb
  - 
  - Required model data:
  -   resourceContext
  -  
  - Optional model data:
  -   
  -->
<#import "vortikal.ftl" as vrtx />
  
<#macro breadCrumb crumbs>

<#if !crumbs?exists>
  <#stop "Unable to render model: required submodel
  'breadcrumb' missing">
</#if>

<#if crumbs?size &gt; 0>
<div class="breadcrumb">
  <span class="breadcrumb-prefix"><@vrtx.msg code="breadcrumb.locationTitle" default="You are here"/>:</span>
  <#list crumbs as elem>
    <#assign name = elem.title/>
    <#if elem.URL?exists>
      <a href="${elem.URL?html}">${name?html}</a> ${elem.delimiter?if_exists?html}
    <#else>
      ${name?html} ${elem.delimiter?if_exists?html}
    </#if>
  </#list>
</div>
</#if>

</#macro>
