<#ftl strip_whitespace=true>

<#--
  - File: view-collectionlisting.ftl
  - 
  - Description: A HTML page that displays a collection listing.
  - 
  - Required model data:
  -   resourceContext
  -   collectionListing:
  -     collections
  -     files
  -     urls
  -     sortURLs
  -     sortProperty
  -  
  - Optional model data:
  -
  -->


<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/dump.ftl" as dumper>

<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>
<#if !collectionListing?exists>
  <#stop "Unable to render model: required submodel
  'collectionListing' missing">
</#if>

<#assign resource = collectionListing.resource />
<#assign resources = collectionListing.files />
<#assign collections = collectionListing.collections />

<#assign title = vrtx.propValue(resource, "userTitle", "flattened") />
<#if resource.URI = '/'>
  <#assign title = resourceContext.repositoryId />
</#if>
<#assign h1 = title />

<#if title == "">
  <#assign title = vrtx.getMsg("article.missingTitle") />
  <#assign h1 = title />
</#if>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
      <link rel="stylesheet" href="${cssURL}">
    </#list>
  </#if>
  <#if collectionFeedURL?exists>
    <#--
    <link rel="alternate" type="application/atom+xml"
          href="${collectionFeedURL.url?html}"
          title="<@vrtx.msg code='viewCollectionListing.feed' args=[resourceContext.currentResource.title] />" />
    -->
  </#if>
  <title>${title?html}</title>
</head>
<body>
    <h1>${h1}</h1> 

    <#-- Image --> 

    <#assign imageRes = vrtx.propResource(resource, "picture") />
    <#assign introductionImage = vrtx.propValue(resource, "picture") />
    <#if introductionImage != "">
      <#if imageRes == "">
        <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
      <#else>

        <#assign userTitle = vrtx.propValue(imageRes, "userTitle", imageRes) />
        <#assign desc = imageRes.getValueByName("description")?default("") />

	<#if userTitle == "" && desc == "">  
          <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
	<#else>
          <#assign pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
          <#assign style="" />
          <#if pixelWidth != "">
            <#assign style = "width:" + pixelWidth+ "px;" />
          </#if>
	 	 
          <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
	    <#if userTitle != "">
	      <img src="${introductionImage}" alt="${userTitle?html}" />
	    <#else>
	      <img src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
	    </#if>
            <div class="vrtx-imagetext">
	      <#if userTitle != "">
		<span class="vrtx-imagetitle">${userTitle?html}<#if desc != "">: </#if></span>
	      </#if>
	      <#if desc != "">
		<span class="vrtx-imagedescription">${desc?html}</span>
	      </#if>
	    </div> 
	  </div>
	</#if>
      </#if>
    </#if>

    <#-- Introduction --> 

    <#assign introduction = vrtx.propValue(resource, "introduction") />
    <#if introduction != "">
      <div class="vrtx-introduction">
        ${introduction}
      </div>
    </#if>


    <#assign description = vrtx.propValue(resource, "introduction", "", "") />
    <#if !description?has_content>
      <#assign description = vrtx.propValue(resource, "description", "", "content") />
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
         <li><a href="${c.getURI()?html}">${vrtx.propValue(c, "title", "" "")?html}</a></li>
       </#list>                                                                                          
     </ul></td></tr>
    </table>
    </div>
  </#if>

  <#-- List resources -->

  <#if resources?size &gt; 0>
    <div class="vrtx-resources">
    <!-- h2><@vrtx.msg code="viewCollectionListing.resources" default="Resources"/></h2 -->
    
    <#--
    <p class="sort">
      <span class="label"><@vrtx.msg code="viewCollectionListing.sortBy" default="Sort by"/>:</span>
      <#if true && collectionListing.sortProperty = 'last-modified'>
        <a href="${collectionListing.sortURLs['title']?html}">
          <@vrtx.msg code="viewCollectionListing.title" default="Title"/></a> |
        <span class="vrtx-active-sort">
          <@vrtx.msg code="viewCollectionListing.lastModified" default="Last Modified"/></span>
      <#else>
        <span class="vrtx-active-sort">
        <@vrtx.msg code="viewCollectionListing.title" default="Title"/></span> |
      <a href="${collectionListing.sortURLs['last-modified']?html}">
        <@vrtx.msg code="viewCollectionListing.lastModified" default="Last Modified"/></a>
      </#if>
    </p>
    -->
    <#list resources as r>
      <div class="vrtx-resource">
        <#if collectionListing.urls[r.URI]?exists>
          <h3><a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${vrtx.propValue(r, "title", "" "")?html}</a></h3>
        <#else>
          <h3>${resourceTitle(r)}</h3>
        </#if>

        <#assign description = vrtx.propValue(r, "description", "content", "") />
        <#if description?has_content>
          <p class="vrtx-description">
            ${description?html}
          </p>
        </#if>
        <div class="vrtx-footer">
          <span class="vrtx-last-modified"><@vrtx.msg code="viewCollectionListing.lastModified" default="Last modified"/>: ${vrtx.propValue(r, "lastModified", "dd.MM.yyyy")?html}</span>
        </div>
      </div>
    </#list>
   </div>
  </#if>
  
  <#if collectionListing.prevURL?exists>
    <a href="${collectionListing.prevURL?html}">previous</a>&nbsp;
  </#if>

  <#if collectionListing.nextURL?exists>
    <a href="${collectionListing.nextURL?html}">next</a>
  </#if>

  </body>
</html>
