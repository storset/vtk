<#import "../vortikal.ftl" as vrtx />

<#macro displayPersons personListing>

  <#local persons=personListing.files />
  <#if persons?size &gt; 0>
    <div id="${personListing.name}" class="vrtx-resources ${personListing.name}">
    <#if personListing.title?exists && collectionListing.offset == 0>
      <h2>${personListing.title?html}</h2>
    </#if>
    
   <table>
    	<tr>
	    	<th>${vrtx.getMsg("person-listing.name")}</th>
	    	<th>${vrtx.getMsg("person-listing.phone")}</th>
	    	<th>${vrtx.getMsg("person-listing.email")}</th>
	    	<th>${vrtx.getMsg("person-listing.tags")}</th>
    	</tr>
    
    <#list persons as person>
    
      <#local title = vrtx.propValue(person, 'title') />
      <#local picture = vrtx.propValue(person, 'picture')  />
      <#local position = vrtx.propValue(person, 'position')  />
      <#local phone = vrtx.propValue(person, 'phone')  />
      <#local email = vrtx.propValue(person, 'email')  />
      <#local tags = vrtx.propValue(person, 'tags') />  
      <#local src = vrtx.propValue(person, 'picture', 'thumbnail') />
	
	   <tr>      
       <#if introImg?has_content>
         <td>
           <a class="vrtx-image" href="${articles.urls[r.URI]?html}"><img src="${src?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" width="30px" /></a>
         </td>
         </#if>
         <td> <a href="${personListing.urls[person.URI]?html}">${title?html}</a><span>${position?html}</span></td>
         <td>${phone?html}</td>
         <td><a href="mailto:${email?html}">${email?html}</a></td>
         <td>
         	<#-- Get list from property - don't split -->
            <#list tags?split(",") as tag>
            	<a href="?vrtx=tags&tag=${tag?trim?html}">${tag?trim?html}</a>
           	</#list>
            </td>
         </tr>
    </#list>
    </table>
  </#if>

</#macro>