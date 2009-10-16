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
      <#local phonenumers = vrtx.propValue(person, 'phone')  />
      <#local emails = vrtx.propValue(person, 'email')  />
      <#local tags = vrtx.propValue(person, 'tags') />  
      <#local src = vrtx.propValue(person, 'picture', 'thumbnail') />
	   <tr>      
         <td> 
  			<#if src?has_content>
           		<a class="vrtx-image" href="${personListing.urls[person.URI]?html}"><img src="${src?html}" alt="" /></a>
           	</#if>
         	<a href="${personListing.urls[person.URI]?html}">${title?html}</a>
         	<div>${position?html}</div>
         </td>
         <td>
         	<#list phonenumers?split(",") as phone>
         	<div>${phone?html}</div>
         	</#list>
         </td>
         <td>
         	<#list emails?split(",") as email >
         		<div><a href="mailto:${email?html}">${email?html}</a></div>
         	</#list>
         </td>
         <td>
            <#list tags?split(",") as tag>
            	<a href="${"?vrtx=tags&tag="?html}${tag?trim?html}">${tag?trim?html}</a>
           	</#list>
            </td>
         </tr>
    </#list>
    </table>
  </#if>

</#macro>
