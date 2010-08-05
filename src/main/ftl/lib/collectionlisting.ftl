<#--
  - File: collectionlisting.ftl
  - 
  - Description: Directory listing view component.
  -   Produces a table, with columns determined by the model variable
  -   'childInfoItems'. See Java class
  -   'org.vortikal.web.referencedataprovider.CollectionListingProvider'
  -   for the model definition used
  - 
  - Required model data:
  -   resourceContext
  -   collectionListing
  -   
  -  
  - Optional model data:
  -   
  -
  -->

<#import "vortikal.ftl" as vrtx />


<#macro listCollection withForm=false action="" submitActions={}>

<#if !resourceContext.currentResource.collection>
  <#stop "This template only works with collection resources:
  ${resourceContext.currentResource.URI} is not a collection." />
</#if>
<#if !collectionListing?exists>
  <#stop "Unable to render model: required submodel
  'collectionListing' missing">
</#if>

<#if withForm>
  <form name="collectionListingForm" action="${action}" method="post" accept-charset="UTF-8">
</#if>

<table id="vrtx-directoryListing" class="directoryListing">
  <tr class="directoryListingHeader">
   <#list collectionListing.childInfoItems as item>
      <#if collectionListing.sortedBy = item>
        <#if collectionListing.invertedSort>
          <th class="invertedSortColumn item">
        <#else>
          <th class="sortColumn ${item}">
        </#if>
      <#else>
        <th class="${item}">
      </#if>
      <#switch item>
          <#case "last-modified">
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.lastModified" default="Last Modified"/></a>
            <#break>

          <#case "locked">
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.locked" default="Locked (by)"/></a>
            <#break>

          <#case "content-type">
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.contentType" default="Content Type"/></a>
            <#break>

          <#case "owner">
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.owner" default="Owner"/></a>
            <#break>
            
          <#case "permissions">
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.permissions" default="Permissions"/></a>
            <#break>

          <#case "content-length">
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.size" default="Size"/></a>
            <#break>

          <#case "name">
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.${item}" default="${item?cap_first}"/></a>
               <#if withForm>
                 </th><th class="checkbox">
                 <a href="#" class="vrtx-check-all" > <@vrtx.msg code="collectionListing.all" default="All"/></a> | 
                 <a href="#" class="vrtx-uncheck-all"> <@vrtx.msg code="collectionListing.none" default="None"/></a>
               </#if>
            <#break>

          <#default>
            <a href="${collectionListing.sortByLinks[item]?html}" id="${item}">
              <@vrtx.msg code="collectionListing.${item}" default="${item?cap_first}"/></a>

      </#switch>
    </th>
    </#list>
    <#list collectionListing.linkedServiceNames as item>
      <th>
      </th>
    </#list>
  </tr>
  <#if collectionListing.children?size < 1>
    <tr>
      <td style="height:35px;text-align:center;" class="emptycollection" colspan="7">
        <@vrtx.msg code="collectionListing.empty" default="This collection is empty"/>.
      </td>
    </tr>
  </#if>

  <#assign rowType = "odd">
  <#list collectionListing.children as child>
  <tr class="${rowType} ${child.resourceType}">
   <#list collectionListing.childInfoItems as item>
      <#assign class = item >
      <#if item = "locked" && child.lock?exists>
        <#assign class = class + " activeLock">
      </#if>
      <#local restricted = "" />
      <#if item = "permissions">
        <#if child.isReadRestricted() >
          <#assign class = class + " restricted" />
          <#local restricted = "restricted">
        </#if>
      </#if>
      <td class="${class}">
        <#switch item>

          <#case "name">
            <#if collectionListing.browsingLinks[child_index]?exists>
              <#local resourceTypeName = vrtx.resourceTypeName(child) />
              <a href="${collectionListing.browsingLinks[child_index]?html}" title="${resourceTypeName}">
                <span class="authorizedListedResource">${child.name}</span>
              </a>
              <#if withForm>
               </td><td class="checkbox" align="center"><input name="${child.URI?html}" type="checkbox"/>
              </#if>
            <#else>
              <span class="unauthorizedListedResource">${child.name}</span>
              <#if withForm>
                </td><td class="checkbox" align="center">&nbsp;
              </#if>

            </#if>
            <#break>

          <#case "content-length">
            <#if child.isCollection()>
              &nbsp;
            <#elseif child.contentLength <= 1000>
              ${child.contentLength} B
            <#elseif child.contentLength <= 1000000>
              ${(child.contentLength / 1000)?string("0.#")} KB
            <#elseif child.contentLength <= 1000000000>
              ${(child.contentLength / 1000000)?string("0.#")} MB
            <#elseif child.contentLength <= 1000000000000>
              ${(child.contentLength / 1000000000)?string("0.#")} GB
            <#else>
              ${child.contentLength} B
            </#if>
            <#break>

          <#case "last-modified">
            <#--${child.lastModified?string("yyyy-MM-dd HH:mm:ss")}-->
            ${child.lastModified?string("yyyy-MM-dd")}
            <#break>

          <#case "locked">
            <#if child.lock?exists>
              <span class="lockOwner"></span>
              <!-- span class="lockOwner">${child.lock.principal.name}</span -->
            </#if>
            <#break>

          <#case "content-type">
            <#if child.contentType != "application/x-vortex-collection">
              ${child.contentType}
            </#if>
            <#break>

          <#case "owner">
            ${child.owner.name}
            <#break>
            
          <#case "permissions">
             <#if restricted != "restricted" >
               ${vrtx.getMsg("collectionListing.permissions.readAll")}
             <#else>
               ${vrtx.getMsg("collectionListing.permissions.restricted")}
             </#if>
            <#break>
        </#switch>
      </td>
    </#list>
    <#list collectionListing.linkedServiceNames as item>
      <td class="${item}">
        <#if collectionListing.childLinks[child_index][item]?has_content>
        <#assign actionName =
                 vrtx.getMsg("collectionListing.action." + item, item, [item, child.name]) />
        <#assign confirmation =
                 vrtx.getMsg("collectionListing.confirmation." + item,
                             "Are you sure you want to " + item + " " + child.name + "?", 
                             [child.name]) />
        <#if child.isCollection()>
          <#assign titleMsg = vrtx.getMsg("confirm-delete.title.folder") />
        <#else>
          <#assign titleMsg = vrtx.getMsg("confirm-delete.title.file") />
        </#if>
      	(&nbsp;<a href="${collectionListing.childLinks[child_index][item]?html}&amp;showAsHtml=true&amp;height=80&amp;width=230"
      	   class="thickbox" title="${titleMsg}">${actionName}</a>&nbsp;)
	</#if>
     </td>
    </#list>
  </tr>
  <#if rowType = "even">
    <#assign rowType = "odd">
  <#else>
    <#assign rowType = "even">
  </#if>
  </#list>
</table>

<#if withForm>
  <div id="collectionListing.submit">
    <#list submitActions?keys as actionName>
      <input type="submit"
             value="${actionName?html}"
             id="collectionListing.action.${actionName?html}"
             name="action"
             title="${submitActions[actionName]?html}" />
      <#--
      <button type="submit" value="${actionName?html}" 
              id="collectionListing.action.${actionName?html}" name="action">
        ${submitActions[actionName]?html}
      </button>
      -->
    </#list>
  </div>
  </form>
</#if>

</#macro>
