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
  
<#macro breadCrumb crumbs downcase=false hidePrefix=false stopLevel=0 startLevel=1>

<#if !crumbs?exists>
  <#stop "Unable to render model: required submodel
  'breadcrumb' missing">
</#if>

<#assign crumbsSize = crumbs?size />

<#if crumbsSize &gt; 0>
<div id="vrtx-breadcrumb-wrapper">
  <div id="vrtx-breadcrumb" class="breadcrumb">
    <#if !hidePrefix>
      <span class="breadcrumb-prefix"><@vrtx.msg code="breadcrumb.locationTitle" default="You are here"/>:</span>
    </#if>
    <#assign counter = startLevel />
    <#list crumbs as elem>
      <#assign name = elem.title/>
      <#if downcase>
        <#assign name = name?lower_case/>
      </#if>
      <#if elem.URL?exists>
        <#if (elem_index == (crumbsSize - 2))>
          <span class="vrtx-breadcrumb-level-${counter?html} vrtx-breadcrumb-before-active"><a href="${elem.URL?html}">${name?html}</a>        
        <#else>
          <span class="vrtx-breadcrumb-level-${counter?html}"><a href="${elem.URL?html}">${name?html}</a>        
        </#if>
        <#if elem.delimiter?exists>
      	  <span class="vrtx-breadcrumb-delimiter">${elem.delimiter?html}</span>
        </#if>
        </span>
      <#else>
        <span class="vrtx-breadcrumb-level-${counter?html} vrtx-breadcrumb-active">${name?html}
        <#if elem.delimiter?exists>
      	  <span class="vrtx-breadcrumb-delimiter">${elem.delimiter?html}</span>
        </#if>
        </span>
      </#if>
      <#if counter = stopLevel>
        <#break>
      </#if>
      <#assign counter = counter+1>
    </#list>
  </div>
</div>
</#if>

</#macro>
