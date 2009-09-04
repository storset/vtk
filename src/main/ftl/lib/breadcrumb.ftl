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
  
<#macro breadCrumb crumbs downcase=false hidePrefix=false stopLevel=0>

<#if !crumbs?exists>
  <#stop "Unable to render model: required submodel
  'breadcrumb' missing">
</#if>

<#if crumbs?size &gt; 0>
<div id="vrtx-breadcrumb" class="breadcrumb">
  <#if !hidePrefix>
      <span class="breadcrumb-prefix"><@vrtx.msg code="breadcrumb.locationTitle" default="You are here"/>:</span>
  </#if>
  <#assign "counter" = 1>
  <#list crumbs as elem>
    <#assign name = elem.title/>
    <#if downcase>
      <#assign name = name?lower_case/>
    </#if>
    <#if elem.URL?exists>
      <span class="vrtx-breadcrumb-level-${counter?html}"><a href="${elem.URL?html}">${name?html}</a>  ${elem.delimiter?if_exists?html}</span>
    <#else>
      <span class="vrtx-breadcrumb-level-${counter?html}">${name?html} ${elem.delimiter?if_exists?html}</span>
    </#if>
    <#if counter = stopLevel>
       <#break>
    </#if>
    <#assign counter = counter+1>
  </#list>
</div>
</#if>

</#macro>
