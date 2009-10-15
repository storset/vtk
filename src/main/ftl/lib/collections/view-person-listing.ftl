<#import "../vortikal.ftl" as vrtx />

<#macro displayPersons personListing>

  <#local persons=personListing.files />
  <#if (persons?size > 0)>  
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
         <td> <#-- fiks feil -->
           <a class="vrtx-image" href="${articles.urls[r.URI]?html}"><img src="${src?html}" alt="${vrtx.getMsg("article.introductionImageAlt")?html}" width="30px" /></a>
         </td>
         </#if>
         <td> <a href="${personListing.urls[person.URI]?html}">${title?html}</a><span>${position?html}</span></td>
         <td>${phone?html}</td>
         <td><a href="mailto:${email?html}">${email?html}</a></td>
         <td>
         	<#-- Get list from property - don't split -->
            <#list tags?split(",") as tag>
            	<a href="${"?vrtx=tags&tag="?html}${tag?trim?html}">${tag?trim?html}</a>
           	</#list>
            </td>
         </tr>
    </#list>
    </table>
  </#if>

</#macro>