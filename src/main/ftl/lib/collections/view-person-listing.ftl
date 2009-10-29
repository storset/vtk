<#import "../vortikal.ftl" as vrtx />

<#macro displayPersons personListing>
  <#local persons=personListing.files />
  <#if (persons?size > 0)>  
   <table class="vrtx-person-listing">
       <thead>
    	 <tr>
	      <th class="vrtx-person-listing-name">${vrtx.getMsg("person-listing.name")}</th>
	      <th class="vrtx-person-listing-phone">${vrtx.getMsg("person-listing.phone")}</th>
	      <th class="vrtx-person-listing-email">${vrtx.getMsg("person-listing.email")}</th>
	      <th class="vrtx-person-listing-tags">${vrtx.getMsg("person-listing.tags")}</th>
    	</tr>
       </thead>
       <tbody>
       <#assign personNr = 1 />
    <#list persons as person>
      <#local title = vrtx.propValue(person, 'title') />
      <#local picture = vrtx.propValue(person, 'picture')  />
      <#local position = vrtx.propValue(person, 'position')  />
      <#local phonenumbers = vrtx.propValue(person, 'phone')  />
      <#local mobilenumbers = vrtx.propValue(person, 'mobile')  />
      <#local emails = vrtx.propValue(person, 'email')  />
      <#local tags = vrtx.propValue(person, 'tags') />  
      <#local src = vrtx.propValue(person, 'picture', 'thumbnail') />
	   <tr class="vrtx-person-${personNr}">      
         <td class="vrtx-person-listing-name"> 
  			<#if src?has_content>
           		<a class="vrtx-image" href="${personListing.urls[person.URI]?html}"><img src="${src?html}" alt="" /></a>
           	</#if>
         	<a href="${personListing.urls[person.URI]?html}">${title?html}</a>
         	<span>${position?html}</span>
         </td>
         <td class="vrtx-person-listing-phone">
            <#if phonenumbers != "" >
              <#list phonenumbers?split(",") as phone>
         	    <span>${phone?html}</span>
         	  </#list>
         	</#if>
            <#if mobilenumbers != "" >
         	  <#list mobilenumbers?split(",") as mobile>
                <span>${mobile?html}</span>
              </#list>
            </#if>
         </td>
         <td class="vrtx-person-listing-email">
            <#if emails != "" >
         	  <#list emails?split(",") as email >
         	    <a href="mailto:${email?html}">${email?html}</a>
         	  </#list>
         	</#if>
         </td>
         <td class="vrtx-person-listing-tags">
            <#assign tagsList = tags?split(",")>
            <#assign nr = 0 />
            <#if tags != "">
              <#list tagsList as tag>
                <#assign nr = nr+1 />
                <a href="${"?vrtx=tags&tag="?html}${tag?trim?html}">${tag?trim?html}</a><#if tagsList?size != nr>,</#if> 
           	  </#list>
           	</#if>
         </td>
       </tr>
       <#assign personNr = personNr+1 />
    </#list>
    </tbody>
  </table>
  </#if>

</#macro>
