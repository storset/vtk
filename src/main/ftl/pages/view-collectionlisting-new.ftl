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
  -  
  - Optional model data:
  -   
  -
  -->

<!#-- TODO:

- show title insted of name if property exists
- sort by; name, lastModified (other?)
- split collections in two column if size &gt; 5 (?)

- need: 
- seperated lists for resources and collections
- map with uri and viewUri
- map with properties for each resource
- messages based on currentResource's contentLocale insted of application language (locale resolver).

-->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/breadcrumb.ftl" as brdcrumb />


<#if !resourceContext.currentResource.collection>
  <#stop "This template only works with collection resources:
  ${resourceContext.currentResource.URI} is not a collection." />
</#if>
<#if !collectionListing?exists>
  <#stop "Unable to render model: required submodel
  'collectionListing' missing">
</#if>


<#-- import "/lib/collectionlisting.ftl" as col / -->

<#assign resources = []/>
<#assign collections = []/>

<#list collectionListing.children as child>
  <#if child.collection>
    <#assign collections = collections + [child] />
   <#else>
    <#assign resources = resources + [child] />
  </#if>
</#list>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>${resourceContext.currentResource.name}</title>
  <style type="text/css">

    .breadcrumb {font-size: 90%;}
    .collections {
      border: 1px solid darkgrey;
      padding: 0px 1em;
      margin: 0px 1em 0px 0px;
    }
    .collections h2 {
      font-size: 110%;
    }
    .collections ul {
      padding: 0px 1em;
      margin-right: 3em;
      float: left;
    }
    .collections ul li{
      padding: 0.1em;
    }


    .resources {
    
    }

    .resources h2 {
      font-size: 130%;
    }
    .resources ul {
      list-style: none;
      padding: 0px 1em;
      margin: 0px;
    }
    .resources ul li{
      padding: 0.1em;
      margin-left: -1.1em;
    }

    .resources ul li div{
      font-size: 90%;
      color: #555;
    }

    p.sort {
    float:right;
    margin: -2.5em 2em 1em 1em;
    font-size: 90%
    }

  </style>
</head>
<body>
  <p class="breadcrumb"><@brdcrumb.breadCrumb crumbs=breadcrumb /></p>

  <h1>${resourceContext.currentResource.name}</h1>

  <#if collections?size &gt; 1>

    <#if collections?size &gt; 5>
       <#assign splitList = ((collections?size/2)+0.5)?int />
    <#else>
      <#assign splitList = -1 />
    </#if>

    <div class="collections">
     <h2><@vrtx.msg code="collectionListing.subfolders" default="Subfolders"/></h2>
     <ul>
       <#list collections as c>
         <#if c_index = splitList>
           </ul><ul>
         </#if>
         <li><a href="${c.getURI()?html}">${c.name}</a></li>
       </#list>                                                                                          
     </ul>
     <div style="visibility:hidden; clear:both">&nbsp;</div>
    </div>
  </#if>

  <#if resources?size &gt; 1>
    <div class="resources">
    <h2><@vrtx.msg code="collectionListing.resources" default="Resources"/></h2>
    <#assign item="name" />
    <p class="sort">
      <@vrtx.msg code="collectionListing.sortBy" default="Sort by"/>: 
      <#switch item>
        <#case "last-modified">
          <a href="${collectionListing.sortByLinks[item]?html}">
            <@vrtx.msg code="collectionListing.${item}" default="${item?cap_first}"/></a> -
          <a href="${collectionListing.sortByLinks[item]?html}">
            <@vrtx.msg code="collectionListing.lastModified" default="Last Modified"/></a>
          <#break>
        <#default>
          <a href="${collectionListing.sortByLinks[item]?html}">
            <@vrtx.msg code="collectionListing.${item}" default="${item?cap_first}"/></a> -
          <a href="${collectionListing.sortByLinks[item]?html}">
            <@vrtx.msg code="collectionListing.lastModified" default="Last Modified"/></a>
          <#break>
      </#switch>
    </p>
    <ul>
      <#list resources as r>
        <li><a href="${r.getURI()?html}">${r.name}</a> 
          <div><@vrtx.msg code="collectionListing.lastModified" default="Last Modified"/> ${r.lastModified?string("dd.MM.yyyy")}</div>
        </li>
      </#list>                                           
    </ul>
   <div>
  </#if>




</body>
</html>

