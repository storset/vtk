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
<#import "/lib/view-collectionlisting.ftl" as coll />
<#import "/lib/dump.ftl" as dumper>

<#assign resource = collection />

<#assign title = vrtx.propValue(resource, "title", "flattened") />


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
      <link rel="stylesheet" href="${cssURL}">
    </#list>
  </#if>
  <#list alternativeRepresentations as alt>
    <link rel="alternate" type="${alt.contentType?html}" title="${alt.title?html}" href="${alt.url?html}" />
  </#list>
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
    <h1>${title}</h1> 

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

     <#assign introduction = coll.getIntroduction(resource) />
     <#if introduction?has_content>
     <div class="vrtx-introduction">
       ${introduction}
     </div>
     </#if>

     <#-- List collections: -->

     <#if subCollections?size &gt; 0>
       <#if subCollections?size &gt; 15>
          <#assign splitList = ((subCollections?size/4)+0.75)?int />
          <#assign interval = splitList />
       <#elseif subCollections?size &gt; 8>
          <#assign splitList = ((subCollections?size/3)+0.5)?int />
          <#assign interval = splitList />
       <#elseif subCollections?size &gt; 3>
          <#assign splitList = ((subCollections?size/2)+0.5)?int />
          <#assign interval = splitList />
       <#else>
         <#assign splitList = -1 />
       </#if>
       
       <#-- List subareasbox _only_ when collection contains at least on subarea that is not hidden -->
       
       <#assign containsUnhidden = "" />
       <#list subCollections as c>
         <#if vrtx.propValue(c, "hidden", "", "navigation") == "" >
           <#assign containsUnhidden = "true" />
           <#break>
         </#if>
       </#list>
       
       <#if containsUnhidden == "true">
         <div class="vrtx-collections">
           <h2><@vrtx.msg code="viewCollectionListing.subareas" default="Subareas"/></h2>
             <ul>
	           <#list subCollections as c>
	             <#if c_index = splitList>
	               <#assign splitList = splitList + interval />
	             </#if>
	             <#if !(vrtx.propValue(c, "hidden", "", "navigation") == "true") >
	               <li><a href="${c.getURI()?html}">${vrtx.propValue(c, "title")?html}</a></li>
	             </#if>
	           </#list>
             </ul>
         </div>
       </#if>
     </#if>

     <#-- List resources: -->

     <#list searchComponents as model>
       <#if collection.resourceType = 'event-listing'>
         <@coll.displayEvents model=model.name />
       <#elseif collection.resourceType = 'article-listing'>
         <@coll.displayResources model=model.name displayMoreURLs=true />
       <#else>
         <@coll.displayResources model=model.name />
       </#if>
     </#list>

  </body>
</html>
