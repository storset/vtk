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
<@vrtx.msg code="breadcrumb.locationTitle" default="You are here"/>:
  <#list crumbs as elem>
    <#assign name = elem.title/>
    <#if name == '/'>
      <#assign name = vrtx.getMsg("breadcrumb.top", "Top")/>
    </#if>
    <#if elem.URL?exists>
      <a href="${elem.URL?html}">${name}</a> &gt;
    <#else>
      ${name} &gt;
    </#if>
  </#list>
    ${resourceContext.currentResource.name?html}
</div>
</#if>

</#macro>
