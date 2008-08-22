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
  
  <#--Gets page from lib/view-collectionlisiting.ftl -->
  <#list searchComponents as searchComponent>
     <#assign page = coll.getPage(searchComponent)>
  </#list>
  
  <#-- TODO: Needs to differ on language NO=" - Side 1,2,3,..." EN=" - Page 1,2,3,..." -->
  <title>${title?html} - <@vrtx.msg code="viewCollectionListing.page" /> <#if page?has_content>${page}</#if></title>
  
</head>
<body>
  <#-- TODO: Needs to differ on language. See above. -->
  <h1>${title} - <@vrtx.msg code="viewCollectionListing.page" /> <#if page?has_content>${page}</#if></h1> 

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


       <div class="vrtx-collections">
         <h2><@vrtx.msg code="viewCollectionListing.subareas" default="Subareas"/></h2>
         <table>
           <tr>
             <td> 
               <ul>
                 <#list subCollections as c>
                   <#if c_index = splitList>
                 </ul></td>
                 <td><ul>
                     <#assign splitList = splitList + interval />
                   </#if>
                   <li><a href="${c.getURI()?html}">${vrtx.propValue(c, "title")?html}</a></li>
                 </#list>                                                                                          
           </ul></td></tr>
         </table>
       </div>

     </#if>

     <#-- List resources: -->

     <#if collection.resourceType = 'event-listing'>
       <#assign upcomingEvents = searchComponents[0] />
       <#assign previousEvents = searchComponents[1] />
       
       <#if previousEvents.prevURL?exists>
         <@coll.displayEvents collectionListing=previousEvents displayMoreURLs=true />
       <#else>
         <@coll.displayEvents collectionListing=upcomingEvents displayMoreURLs=true />
         <@coll.displayEvents collectionListing=previousEvents displayMoreURLs=true />
       </#if>
     <#else>
       <#list searchComponents as searchComponent>
         <#if collection.resourceType = 'article-listing'>
           <@coll.displayArticles collectionListing=searchComponent displayMoreURLs=true />
         <#else>
           <@coll.displayResources collectionListing=searchComponent />
         </#if>
       </#list>
     </#if>
     

    <#-- XXX: display first link with content type = atom: -->
    <#list alternativeRepresentations as alt>
      <#if alt.contentType = 'application/atom+xml'>
        <div class="vrtx-feed-link">
          <a href="${alt.url?html}"><@vrtx.msg code="viewCollectionListing.feed.fromThis" /></a>
        </div>
        <#break />
      </#if>
    </#list>
  </body>
</html>
