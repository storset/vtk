<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro addScripts collection>

    <#if cssURLs?exists>
    <@addScriptURLs "css" "common" cssURLs />
    <@addScriptURLs "css" listingType cssURLs />
  </#if>
  <#if jsURLs?exists>
    <@addScriptURLs "js" "common" jsURLs />
    <@addScriptURLs "js" listingType jsURLs />
  </#if>
</#macro>

<#macro addScriptURLs scriptType listingType urls>
  
  <#if urls[listingType]?exists>
    <#list urls[listingType] as commonUrl>
      <#if scriptType == "css">
        <link rel="stylesheet" href="${commonUrl}" />
      <#elseif scriptType == "js">
        <script type="text/javascript" src="${commonUrl}"></script>
      </#if>
    </#list>
  </#if>

</#macro>

<#macro displayCollection collectionListing>

  <#local resources=collectionListing.files />
  <#if resources?size &gt; 0>
  <script type="text/javascript"><!--
       $(window).load(function() {
         if($("#right-main").length) {
           var cut = ".last-four";
         } else {
           var cut = ".last-five";
         }
	     $('ul.vrtx-image-listing').find(".vrtx-image-entry:not(" + cut + ")")
	       .css("marginRight", "18px !important;").end()
	       .masonry({singleMode: false});
	   });
     // -->
     </script>
     <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2>
    </#if>
     <div class="vrtx-image-listing-container">
      <ul class="vrtx-image-listing">
      <#assign count = 1 />

    <#list resources as r>
<#if count % 4 == 0 && count % 5 == 0>
          <li class="vrtx-image-entry last last-four last-five">
        <#elseif count % 4 == 0>
          <li class="vrtx-image-entry last last-four">
        <#elseif count % 5 == 0>
          <li class="vrtx-image-entry last-five">
        <#else>
          <li class="vrtx-image-entry">
        </#if>
     <#local contentType = vrtx.propValue(r, 'contentType') />
     <div class="vrtx-image-container">
    <#if contentType == "audio" || contentType == "audio/mpeg" || contentType == "audio/mp3">
    <img src="/vrtx/__vrtx/static-resources/themes/default/icons/audio-icon.png" />
    <#elseif contentType == "video/x-flv"  || contentType == "video/mp4">
    <img src="/vrtx/__vrtx/static-resources/themes/default/icons/video-icon.png" />
    </#if>
    </div>
      <div class="vrtx-image-info">
              <div class="vrtx-image-title">
        <a class="vrtx-title" href="${collectionListing.urls[r.URI]?html}">${vrtx.propValue(r, "title", "", "")?html}</a>
		</div>
        <#list collectionListing.displayPropDefs as displayPropDef>

          <#if displayPropDef.name = 'introduction'>
            <#assign val = vrtx.getIntroduction(r) />
          <#elseif displayPropDef.type = 'IMAGE_REF'>
            <#assign val><img src="${vrtx.propValue(r, displayPropDef.name, "")}" /></#assign>
          <#local creationTime = vrtx.propValue(r, 'creationTime', 'short', '') />
              <div class="vrtx-image-creation-time">
                ${creationTime}
              </div>

          <#else>
            <#assign val = vrtx.propValue(r, displayPropDef.name, "short") />
          </#if>

          <#if val?has_content>
            <div class="${displayPropDef.name}">
              ${val}
            </div>            
          </#if>
        </#list>
</div>
      </div>
      </li>
      <#assign count = count +1 />
    </#list>
</ul>
   </div>
   </#if>
</#macro>
