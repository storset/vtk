<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local messages = collectionListing.files />
  <#local editLinks = collectionListing.editLinkAuthorized />

  <#if (messages?size > 0)>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
      <#if collectionListing.title?exists && collectionListing.offset == 0>
        <h2>${collectionListing.title?html}</h2>
      </#if>

      <@displayMessages messages editLinks />

    </div>
  </#if>

</#macro>

<#macro displayMessages messages editLinks=[] >
  <#local i = 1 />
  <#list messages as message>
    <#local locale = vrtx.getLocale(message) />
    <#local uri = vrtx.getUri(message) />
    <div id="vrtx-result-${i}" class="vrtx-resource">
      <div class="vrtx-title">
        <#assign title = vrtx.propValue(message, "title") />
        <#if !title?has_content>
          <#assign title = vrtx.propValue(message, "solr.name") />
        </#if>
        <a class="vrtx-title" href="${uri?html}">${title?html}</a>
        <#if editLinks?exists && editLinks[message_index]?exists && editLinks[message_index]>
          <a class="vrtx-message-listing-edit" href="${vrtx.relativeLinkConstructor(uri, 'simpleMessageEditor')}"><@vrtx.msg code="collectionListing.editlink" /></a>
        </#if> 
      </div>
      <div class="published-date">
        <span class="published-date-prefix">
          <@vrtx.localizeMessage code="viewCollectionListing.publishedDate" default="" args=[] locale=locale />
        </span>
        <#local publishDateProp = vrtx.prop(message, 'publish-date') />
        <@vrtx.date value=publishDateProp.dateValue format='long' locale=locale />
      </div>
      <div class="description introduction">
        <#assign messageIntro = vrtx.propValue(message, "listingDisplayedMessage", "", "") />
        <#if messageIntro?exists>
          ${messageIntro}
          <#assign isTruncated = vrtx.propValue(message, "isTruncated", "", "") />
          <#if isTruncated?exists && isTruncated = 'true'>
            <div class="vrtx-read-more">
            <a href="${message.URI?html}" class="more">
              <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
            </a>
            </div>
          </#if>
        </#if>
      </div>
    </div>
    <#local i = i + 1 />
  </#list>
</#macro>
