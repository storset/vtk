<#--
  - File: header.ftl
  - 
  - Description: Default manage header 
  - 
  - Required model data:
  -   manageBreadCrumb
  -
  -->
  <#import "/lib/breadcrumb.ftl" as brdcrmb/>

<#if !manageBreadCrumb?exists>
  <#stop "Unable to render model: required submodel
  'manageBreadcrumb' missing">
</#if>

  <#include "banner.ftl"/>

  <div class="body">
    <@brdcrmb.breadCrumb crumbs=manageBreadCrumb />
    
    <#include "message.ftl"/>

    <#include "resource-title.ftl"/>

    <#include "tabs.ftl"/>

    <div id="main">

      <#include "tabline.ftl"/>

      <div id="contents">
