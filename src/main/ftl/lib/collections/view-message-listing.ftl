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

<#macro displayMessages messages editLinks=[] componentView=false compactView=false >
  <#local i = 1 />
  <#if compactView><div class="vrtx-feed"><ul class="items"></#if>
  <#list messages as message>
    <#local locale = vrtx.getLocale(message) />
    <#local uri = vrtx.getUri(message) />
    <#local title = vrtx.propValue(message, "title") />
    <#if !title?has_content>
      <#local title = vrtx.propValue(message, "solr.name") />
    </#if>

    <#if compactView> <#-- XXX: use feed-component instead (but need to avoid another search)? -->
      <li class="item-${i}">
        <a class="item-title" href="${uri?html}">${title?html}</a>
        <#local publishDateProp = vrtx.prop(message, 'publish-date') />
        <span class="published-date"><@vrtx.date value=publishDateProp.dateValue format='long' locale=locale /></span>
      </li>
      
    <#else>
      <div class="vrtx-result-${i} vrtx-resource">
        <div class="vrtx-title">
          <a class="vrtx-title" href="${uri?html}">${title?html}</a>
          <#if editLinks?exists && editLinks[message_index]?exists && editLinks[message_index]>
            <a class="vrtx-message-listing-edit" href="${vrtx.relativeLinkConstructor(uri, 'simpleMessageEditor')}"><@vrtx.msg code="collectionListing.editlink" /></a>
          </#if> 
        </div>
        <#if componentView>
          <#local messageIntro = vrtx.propValue(message, "listingDisplayedMessage", "", "") />
          <#if messageIntro??>
            <div class="description introduction">
              ${messageIntro}
             </div>
          </#if>
      
          <div class="vrtx-message-line">
            <span class="vrtx-message-line-last-modified-by">
              <#-- TODO:
              <#local modifiedBy = vrtx.prop(message, 'modifiedBy').principalValue />
              <#if principalDocuments?? && principalDocuments[modifiedBy.name]??>
                <#local principal = principalDocuments[modifiedBy.name] />
                <#if principal.URL??>
                  <#local val = val + " <a href='${principal.URL}'>${principal.description}</a>" />
                <#else>
                  <#local val = val + " ${principal.description}" />
                </#if>
              <#else>
                <#local val = val + " " + vrtx.propValue(message, 'modifiedBy', 'link') />
              </#if>
              ${val}-->
              Odd roger
            </span>
            <span class="vrtx-message-line-middle-fix"> - </span>
            <span class="vrtx-message-line-last-modified-date">
              <#local lastModifiedDateProp = vrtx.prop(message, 'lastModified') />
              <@vrtx.date value=lastModifiedDateProp.dateValue format='long' locale=locale />
            </span>
            <#local numberOfComments = vrtx.prop(message, "numberOfComments") />
            <#if numberOfComments?has_content >
              <span class="vrtx-message-line-number-of-comments">
                <@viewutils.displayNumberOfComments message locale />
              </span>
            </#if>
          </div>

          <#local isTruncated = vrtx.propValue(message, "isTruncated", "", "") />
          <#if isTruncated?exists && isTruncated = 'true'>
            <div class="vrtx-read-more">
              <a href="${message.URI?html}" class="more">
                <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
              </a>
            </div>
          </#if> 
        <#else>
          <div class="published-date">
            <span class="published-date-prefix">
              <@vrtx.localizeMessage code="viewCollectionListing.publishedDate" default="" args=[] locale=locale />
            </span>
            <#local publishDateProp = vrtx.prop(message, 'publish-date') />
            <@vrtx.date value=publishDateProp.dateValue format='long' locale=locale />
          </div>

          <#local numberOfComments = vrtx.prop(message, "numberOfComments") />
          <#if numberOfComments?has_content >
            <div class="vrtx-number-of-comments-add-event-container">
              <@viewutils.displayNumberOfComments message locale />
            </div>
          </#if>

          <div class="description introduction">
            <#local messageIntro = vrtx.propValue(message, "listingDisplayedMessage", "", "") />
            <#if messageIntro??>
              ${messageIntro}
              <#local isTruncated = vrtx.propValue(message, "isTruncated", "", "") />
              <#if isTruncated?exists && isTruncated = 'true'>
                <div class="vrtx-read-more">
                  <a href="${message.URI?html}" class="more">
                    <@vrtx.localizeMessage code="viewCollectionListing.readMore" default="" args=[] locale=locale />
                  </a>
                </div>
              </#if>
            </#if>
          </div>
        </#if>
      </div>
    </#if>
    <#local i = i + 1 />
  </#list>
   <#if compactView></ul></div></#if>
</#macro>