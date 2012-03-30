<#ftl strip_whitespace=true>
<#import "../vortikal.ftl" as vrtx />
<#macro displayPersons personListing title="">
  <#local persons=personListing.files />
  <#if (persons?size > 0)>
    <table class="vrtx-person-listing" summary="${vrtx.getMsg("person-listing.overview-of")} ${title?html}">
      <#if numberOfRecords?exists>
        <caption>
          ${vrtx.getMsg("person-listing.persons")} ${numberOfRecords["elementsOnPreviousPages"]} -
          ${numberOfRecords["elementsIncludingThisPage"]} ${vrtx.getMsg("person-listing.of")} 
          ${personListing.totalHits?string}
        </caption>
      </#if>
      <thead>
        <tr>
          <th class="vrtx-person-listing-name">${vrtx.getMsg("person-listing.name")}</th>
          <th class="vrtx-person-listing-phone">${vrtx.getMsg("person-listing.phone")}</th>
          <th class="vrtx-person-listing-email">${vrtx.getMsg("person-listing.email")}</th>
          <th class="vrtx-person-listing-tags">${vrtx.getMsg("person-listing.tags")}</th>
        </tr>
      </thead>
      <tbody>
    <#local personNr = 1 />
    <#list persons as person>
      <#local firstName = vrtx.propValue(person, 'firstName') />
      <#local surname = vrtx.propValue(person, 'surname') />
      <#local title = vrtx.propValue(person, 'title') />
      <#local picture = vrtx.propValue(person, 'picture')  />
      <#local position = vrtx.propValue(person, 'position')  />
      <#local phonenumbers = vrtx.propValue(person, 'phone')  />
      <#local mobilenumbers = vrtx.propValue(person, 'mobile')  />
      <#local emails = vrtx.propValue(person, 'email')  />
      <#local tags = vrtx.propValue(person, 'tags') />
      
      <#local src = vrtx.propValue(person, 'picture', 'thumbnail') />
      
      <#local introImgURI = vrtx.propValue(person, 'picture') />
      <#if introImgURI?exists>
    		<#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
      <#else>
        <#local thumbnail = "" />
   	  </#if>
      
      <#local imageAlt = vrtx.getMsg("person-listing.image-alt") >
      <#local imageAlt = imageAlt + " " + firstName + " " + surname />
	  <tr class="vrtx-person-${personNr}">
        <td class="vrtx-person-listing-name">
          <#if src?has_content>
            <a class="vrtx-image" href="${person.URI?html}"><img src="${thumbnail?html}" alt="${imageAlt}" /></a>
          </#if>
          <#if surname?has_content >
            <a href="${person.URI?html}">${surname}<#if firstName?has_content && surname?has_content>, </#if>${firstName?html}</a>
          <#else>
            <a href="${person.URI?html}">${title?html}</a>
          </#if>
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
            <#list emails?split(",") as email>
              <#if (email?string?length > 25) >
                <#if email?string?contains('@') >
                  <#assign eS = email?string?split('@') />
                  <a href="mailto:${email?html}"><span>${eS[0]?html}</span><span>@${eS[1]?html}</span></a>
                <#else>
                  <a href="mailto:${email?html}"><span>${email?string?substring(0, 25)?html}</span><span>${email?string?substring(25,email?string?length)?html}</span></a>
                </#if>
              <#else>
                <a href="mailto:${email?html}">${email?html}</a>
              </#if>
            </#list>
          </#if>
        </td>
        <td class="vrtx-person-listing-tags">
          <#local tagsList = tags?split(",")>
          <#local tagsNr = 0 />
          <#if tags != "">
            <#list tagsList as tag>
              <#local tagUrl = "?vrtx=tags&tag=" + tag?trim + "&resource-type=" + person.getResourceType() />
              <#local sortingParams = personListing.getRequestSortOrderParams() />
              <#if sortingParams?has_content>
                <#local tagUrl = tagUrl + "&" + sortingParams />
              </#if>
              <#local tagsNr = tagsNr+1 />
              <a href="${tagUrl?html}">${tag?trim?html}</a><#if tagsList?size != tagsNr>,</#if>
            </#list>
          </#if>
        </td>
      </tr>
      <#local personNr = personNr+1 />
    </#list>
      </tbody>
    </table>
  </#if>
</#macro>