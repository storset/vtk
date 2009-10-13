<#import "../vortikal.ftl" as vrtx />

<#macro displayPersons personListing>

  <#local persons=personListing.files />
  <#if persons?size &gt; 0>
    <div id="${personListing.name}" class="vrtx-resources ${personListing.name}">
    <#if personListing.title?exists && collectionListing.offset == 0>
      <h2>${personListing.title?html}</h2>
    </#if>
    
    <#list persons as person>
    
      <#local title = vrtx.propValue(person, 'title') />
      <#local picture = vrtx.propValue(person, 'picture')  />
      <#local position = vrtx.propValue(person, 'position')  />
      <#local phone = vrtx.propValue(person, 'phone')  />
      <#local email = vrtx.propValue(person, 'email')  />
      <#local tags = vrtx.propValue(person, 'tags') />
    
      <div class="vrtx-person">
        <div class="vrtx-person-title">
          <a class="vrtx-person-title-anchor" href="${personListing.urls[person.URI]?html}">${title?html}</a>
        </div>
      </div>
    </#list>
    </div>
  </#if>

</#macro>