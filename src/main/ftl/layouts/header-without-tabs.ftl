<#--
  - File: header-browse.ftl
  - 
  - Description: browse header
  - 
  - Required model data:
  -   breadcrumb
  -
  -->

<#import "/lib/breadcrumb.ftl" as brdcrmb/>
<#include "banner-new-window.ftl"/>

<#if !breadcrumb?exists>
  <#stop "Unable to render model: required submodel
  'breadcrumb' missing">
</#if>

  <div class="body">

    <#if aboveBreadCrumbMessage?exists>
      <div class="aboveBreadCrumbMessage"><@vrtx.msg code="${aboveBreadCrumbMessage}" default="${aboveBreadCrumbMessage}"/></div>
    </#if>

    <@brdcrmb.breadCrumb crumbs=breadcrumb />

    <#include "resource-title.ftl"/>

