<#ftl strip_whitespace=true>
<#import "../vortikal.ftl" as vrtx />

<#macro displayMastersAlphabetical masterListing>
  <#list alpthabeticalOrdredResult?keys as key >
    <ul class="vrtx-alphabetical-master-listing">
	  <li>${key}
	    <ul>
		  <#list alpthabeticalOrdredResult[key] as master>
			<#local title = vrtx.propValue(master.propertySet, 'title') />
			<li><a href="${master.url?html}">${title}</a></li>
		  </#list>
		</ul>
	  </li>
	</ul>
  </#list>
</#macro>

<#macro masterListingViewServiceURL >
  <#if viewAllMastersLink?exists || viewOngoingMastersLink?exists>
	<div id="vrtx-listing-completed-ongoing">
	  <#if viewAllMastersLink?exists>
	  	<a href="${viewAllMastersLink}">${vrtx.getMsg("masters.viewCompleted")}</a>
	  </#if>
	  <#if viewOngoingMastersLink?exists>
	  	<a href="${viewOngoingMastersLink}">${vrtx.getMsg("masters.viewOngoing")}</a>
	  </#if>
	</div>
  </#if>
</#macro>

<#macro displayMasters masterListing>
  <#local masters = masterListing.entries />
  <#if (masters?size > 0) >
    <div id="${masterListing.name}" class="vrtx-masters ${masterListing.name}">
      <#if masterListing.title?exists && masterListing.offset == 0>
        <h2>${masterListing.title?html}</h2>
      </#if>
      <#local locale = springMacroRequestContext.getLocale() />
      <#list masters as masterEntry>
        <#local master = mesterEntry.propertySet />
        <#local title = vrtx.propValue(master, 'title') />
        <#local introImg = vrtx.prop(master, 'picture')  />
        <#local intro = vrtx.prop(master, 'introduction')  />
        <#local caption = vrtx.propValue(master, 'caption')  />
        <div class="vrtx-master">
          <#if introImg?has_content >
            <#local src = vrtx.propValue(master, 'picture', 'thumbnail') />
            <#local introImgURI = vrtx.propValue(master, 'picture') />
          	<#if introImgURI?exists>
    		  <#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
    	 	<#else>
    		  <#local thumbnail = "" />
   		   	</#if>
   		   	<#local introImgAlt = vrtx.propValue(master, 'pictureAlt') />
            <a class="vrtx-image" href="${masterEntry.url?html}">
              <img src="${thumbnail?html}" alt="<#if introImgAlt?has_content>${introImgAlt?html}</#if>" />
            </a>
          </#if>
          <div class="vrtx-title">
            <a class="vrtx-title summary" href="${masterEntry.url?html}">${title?html}</a>
		  </div>
          <#if intro?has_content && masterListing.hasDisplayPropDef(intro.definition.name)>
            <div class="description introduction">
        	  <@vrtx.linkResolveFilter intro.value masterListing.urls[master.URI] requestURL />
        	</div>
          </#if>
          <div class="vrtx-read-more">
            <a href="${masterEntry.url?html}" class="more">
              <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
            </a>
          </div>
        </div>
      </#list>
    </div>
  </#if>
</#macro>

<#macro displayTable masterListing collection>
  <#local masters = masterListing.entries />
  <#if (masters?size > 0)>
    <div class="vrtx-master-table">
      <table class="sortable" border="1">
        <thead>
          <tr>
            <th id="vrtx-table-title" class="sortable-text">${vrtx.getMsg("property.title")}</th>
            <th id="vrtx-table-creation-time" class="sortable-sortEnglishLonghandDateFormat">${vrtx.getMsg("publish.permission.published")}</th>
            <th id="vrtx-table-scope" class="sortable-text">${vrtx.getMsg("masterListing.scope")}</th>
	      	<th id="vrtx-table-dimensions-height" class="sortable-text">${vrtx.getMsg("masterListing.persons")}</th>
          </tr>
        </thead>
        <tbody>
        <#assign masterCount = 1 />
        <#list masters as masterEntry>
          <#if (masterCount % 2 == 0)>
            <tr id="vrtx-master-${masterCount}" class="even">
          <#else>
            <tr id="vrtx-master-${masterCount}">
          </#if>
            <#local master = masterEntry.propertySet />
            <#local title = vrtx.propValue(master, 'title')?html />
            <td class="vrtx-table-title"><a href="${masterEntry.url}">${title}</a></td>
            <#local publishDate = vrtx.propValue(master, 'publish-date', 'short', '') />
            <td class="vrtx-table-creation-time">${publishDate}</td>
            <td class="vrtx-table-scope">${vrtx.propValue(master, 'credits')?html}</td>
            <td class="vrtx-table-persons">
            
            <#if personsRelatedToMaster?? && personsRelatedToMaster[master]?exists >
		        <ul>
		        <#assign count = 1 />
		        <#assign size = personsRelatedToMaster[master]?size />
			      <#list personsRelatedToMaster[master] as person>
				      <#assign url = vrtx.getMetadata(person, "url") />
				      <#assign surname = vrtx.getMetadata(person, "surname") />
           		<#assign firstName = vrtx.getMetadata(person, "firstName") />
           	    <#assign description = vrtx.getMetadata(person "description") />
           		<#assign name = "" />
			        <#if surname != "" && firstName != "">
			          <#assign name = firstName + " " + surname />
			        <#else>
			          <#assign url = "" />
				        <#assign name = description />
			        </#if>
					   
			        <#if name?exists >
				        <li>
				          <#if url?exists && url != "">
					          <a href="${url?html}">${name?html}<#t/>
					        <#else>
					          ${name?html}<#t/>
					        </#if>
					        <#t/><#if (size > 1 && count < size)>,</#if>
				        </li>
				        <#assign count = count + 1 />
				      </#if>
				    </#list>
			      </ul>
            </#if> 
                        
            </td>
          </tr>
          <#assign masterCount = masterCount + 1 />
        </#list>
        </tbody>
      </table>
    </div>
  </#if>
</#macro>

<#macro completed>
  <#if viewOngoingMastersLink?exists>
    <span>${vrtx.getMsg("masters.listCompleted")}</span>
  </#if>
</#macro>