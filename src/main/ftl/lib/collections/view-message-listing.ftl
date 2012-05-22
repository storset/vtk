<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local resources = collectionListing.files />
  <#local editLinks = collectionListing.editLinkAuthorized />

  <#local i = 1 />
  <#if (resources?size > 0)>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
      <#if collectionListing.title?exists && collectionListing.offset == 0>
        <h2>${collectionListing.title?html}</h2>
      </#if>
      <#list resources as r>
        <#local locale = vrtx.getLocale(r) />
        <#local uri = vrtx.getUri(r) />
        <div id="vrtx-result-${i}" class="vrtx-resource">
		  <div class="vrtx-title">
		    <#assign title = vrtx.propValue(r, "title") />
		    <#if !title?has_content>
		      <#assign title = vrtx.propValue(r, "solr.name") />
		    </#if>
            <a class="vrtx-title" href="${uri?html}">${title?html}</a>
            <#if editLinks?exists && editLinks[r_index]>
              <a class="vrtx-message-listing-edit" href="${vrtx.relativeLinkConstructor(uri, 'simpleMessageEditor')}"><@vrtx.msg code="report.collection-structure.edit" /></a>
            </#if> 
		  </div>
          <div class="published-date">
            <span class="published-date-prefix">
              <@vrtx.localizeMessage code="viewCollectionListing.publishedDate" default="" args=[] locale=locale />
            </span>
            <#local publishDateProp = vrtx.prop(r, 'publish-date') />
            <@vrtx.date value=publishDateProp.dateValue format='long' locale=locale />
          </div>
          <div class="description introduction">
            <#assign message = vrtx.propValue(r, "listingDisplayedMessage", "", "") />
            <#if message?exists>
              ${message}
              <#assign isTruncated = vrtx.propValue(r, "isTruncated", "", "") />
              <#if isTruncated?exists && isTruncated = 'true'>
                <div class="vrtx-read-more">
                <a href="${collectionListing.urls[r.URI]?html}" class="more">
                  <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
                </a>
            </div>
              </#if>
            </#if>
          </div>
        </div>
        <#local i = i + 1 />
      </#list>
    </div>
  </#if>
</#macro>
