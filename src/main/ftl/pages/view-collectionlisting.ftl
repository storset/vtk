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

<#assign title = vrtx.propValue(resource, "title", "flattened") />
<#if resource.URI = '/'>
  <#assign title = resourceContext.repositoryId />
</#if>
<#assign h1 = title />

<#-- Introduction --> 
<#function getIntroduction resource>
  <#local introduction = vrtx.propValue(resource, "introduction") />
  <#if !introduction?has_content>
    <#local introduction = vrtx.propValue(resource, "description", "", "content") />
  </#if>
  <#return introduction />
</#function>

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

  <#assign introduction = getIntroduction(resource) />
  <#if introduction?has_content>
  <div class="vrtx-introduction">
    ${introduction}
  </div>
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
    
    <#list resources as r>
      <div class="vrtx-resource">
        <#if collectionListing.urls[r.URI]?exists>
          <h3><a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${vrtx.propValue(r, "title", "" "")}</a></h3>
        <#else>
          <h3>${vrtx.propValue(r, "title", "" "")}</h3>
        </#if>

        <#list collectionListing.displayPropDefs as displayPropDef>

          <#if displayPropDef.name = 'introduction'>
            <#assign val = getIntroduction(r) />
          <#elseif displayPropDef.type = 'IMAGE_REF'>
            <#assign val><img class="vrtx-prop" src="${vrtx.propValue(r, displayPropDef.name, "")}" /></#assign>
          <#else>
            <#assign val = vrtx.propValue(r, displayPropDef.name, "long") /> <#-- XXX -->
          </#if>

          <#if val?has_content>
            <div class="vrtx-prop ${displayPropDef.name}">
              ${val}
            </div>
          </#if>
        </#list>
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
