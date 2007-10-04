<#ftl strip_whitespace=true>

<#--
  - File: view-collectionlisting.ftl
  - 
  - Description: A HTML page that displays a collection listing.
  - 
  - Required model data:
  -   resourceContext
  -   collectionListing
  -  
  - Optional model data:
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />
<#-- import "/lib/collectionlisting.ftl" as col / -->
<#import "/lib/dump.ftl" as dumper>

<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>

<#if !resourceContext.currentResource.collection>
  <#stop "This template only works with collection resources:
  ${resourceContext.currentResource.URI} is not a collection." />
</#if>

<#if !collectionListing?exists>
  <#stop "Unable to render model: required submodel
  'collectionListing' missing">
</#if>

<#assign resources = collectionListing.files />
<#assign collections = collectionListing.collections />

<#function title r=resourceContext.currentResource>
  <#if r.URI = '/'>
     <#return resourceContext.repositoryId?html>
  <#else>
     <#return r.title?html>
  </#if>
</#function>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
      <link rel="stylesheet" href="${cssURL}">
    </#list>
  </#if>
  <title>${title()}</title>
</head>
<body>
    <h1>${title()}</h1> 

    <#assign description>
      <@vrtx.property resource=resourceContext.currentResource
                      prefix="content" name="description"  />
    </#assign>
    <#if description?has_content>
      <p class="vrtx-description">
        ${description?html}
      </p>
    </#if>

  <#if collections?size &gt; 0>
    <#if collections?size &gt; 15>
       <#assign splitList = ((collections?size/4)+0.75)?int />
       <#assign interval = splitList />
    <#elseif collections?size &gt; 8>
       <#assign splitList = ((collections?size/3)+0.5)?int />
       <#assign interval = splitList />
    <#elseif collections?size &gt; 3>
       <#assign splitList = ((collections?size/2)+0.5)?int />
       <#assign interval = splitList />
    <#else>
      <#assign splitList = -1 />
    </#if>


    <div class="vrtx-collections">
     <h2><@vrtx.msg code="viewCollectionListing.subareas" default="Subareas"/></h2>
     <table>
     <tr>
     <td> 
      <ul>
       <#list collections as c>
         <#if c_index = splitList>
           </ul></td>
            <td><ul>
           <#assign splitList = splitList + interval />
         </#if>
         <li><a href="${c.getURI()?html}">${title(c)}</a></li>
       </#list>                                                                                          
     </ul></td></tr>
    </table>
    </div>
  </#if>

  <#-- List resources -->

  <#if resources?size &gt; 0>
    <div class="vrtx-resources">
    <!-- h2><@vrtx.msg code="viewCollectionListing.resources" default="Resources"/></h2 -->
    <p class="sort">
      <@vrtx.msg code="viewCollectionListing.sortBy" default="Sort by"/>: 
      <a href="${collectionListing.sortURLs['title']?html}">
        <@vrtx.msg code="viewCollectionListing.title" default="Title"/></a> -
      <a href="${collectionListing.sortURLs['last-modified']?html}">
        <@vrtx.msg code="viewCollectionListing.lastModified" default="Last Modified"/></a>
    </p>
    <#list resources as r>
      <div class="vrtx-resource">
        <h3><a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${title(r)}</a></h3>
        <#assign description>
          <@vrtx.property resource=r prefix="content" name="description"  />
        </#assign>
        <#if description?has_content>
          <p class="vrtx-description">
            ${description?html}
          </p>
        </#if>
        <div class="vrtx-footer">
          <span class="vrtx-last-modified"><@vrtx.msg code="viewCollectionListing.lastModified" default="Last modified"/>: ${r.lastModified?string("dd.MM.yyyy")}</span>
        </div>
      </div>
    </#list>
   </div>
  </#if>
  </body>
</html>
