<#ftl strip_whitespace=true>

<#--
  - File: article.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -
  -->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#function propValue propName resource=resource format="long">
<#local prop = resource.getPropertyByPrefix("", propName)?default("") />
<#if prop != "">
  <#local type = prop.definition.type />
  <#if type = 'DATE' || type = 'TIMESTAMP'>
    <#local locale = springMacroRequestContext.getLocale() />
    <#return prop.getFormattedValue(format, locale) />
  <#else>
    <#return prop.formattedValue />
  </#if>

</#if>
<#return "" />
</#function>

<#function propResource propName>
  <#local prop = resource.getPropertyByPrefix("", propName)?default("") />
  <#if prop != "">
    <#local def = prop.definition />
    <#local type = def.type />
    <#if type = 'IMAGE_REF'>
      <#return resource.getPropResource(def)?default("") />
    </#if>
  </#if>
  <#return "" />
</#function>


<#assign title = propValue("userTitle")?html />
<#if title == "">
  <#assign title = vrtx.getMsg("article.missingTitle") />
</#if>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${title}</title>
    <#if cssURL?exists>
      <link rel="stylesheet" type="text/css" media="screen" href="${cssURL?html}" />
    </#if>
</head>
  <body>
    <h1>${title}</h1>

    <#-- Image --> 

    <#assign imageRes = propResource("picture") />
    <#assign introductionImage = propValue("picture") />
    <#if introductionImage != "">
      <#if imageRes == "">
        <img class="vrtx-introduction-image" src="${introductionImage}" alt="vrtx.getMsg("article.introductionImageAlt")" />
      <#else>

        <#assign userTitle = propValue("userTitle", imageRes) />
        <#assign desc = imageRes.getValueByName("description")?default("") />

	<#if userTitle == "" && desc == "">  
          <img class="vrtx-introduction-image" src="${introductionImage}" alt="vrtx.getMsg("article.introductionImageAlt")" />
	<#else>
          <#assign pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
          <#if pixelWidth != "">
            <#assign style = "width:" + pixelWidth+ "px;" />
          </#if>
	 	 
          <div class="vrtx-introduction-image" style="${style}">
	    <#if userTitle != "">
	      <img src="${introductionImage}" alt="${userTitle?html}" />
	    <#else>
	      <img src="${introductionImage}" alt="vrtx.getMsg("article.introductionImageAlt")" />
	    </#if>
            <div class="vrtx-imagetext">
	      <#if userTitle != "">
		<span class="vrtx-imagetitle">${userTitle?html}<#if desc != "">: </#if></span>
	      </#if>
	      <#if desc != "">
		<span class="vrtx-imagedescription">${desc?html}</span>
	      </#if>
	    </div> 
	  </div>
	</#if>
      </#if>
    </#if>

    <#-- Ingress --> 

    <#assign introduction = propValue("introduction") />
    <#if introduction != "">
      <div class="vrtx-introduction">
        ${introduction}
      </div>
    </#if>

    <#-- Media ref --> 

    <#assign mediaRes = propResource("media") />
    <#assign media = propValue("media") />

    <#if mediaRes != "">
      <#if mediaRes.resourceType == 'audio'>
      <script type="text/javascript" language="JavaScript" src="${mediaPlayerBase.url?html}/audio-player.js"></script>
      <object type="application/x-shockwave-flash" data="${mediaPlayerBase.url?html}/player.swf" id="audioplayer1" height="24" width="290">
	<param name="movie" value="${mediaPlayerBase.url?html}/player.swf"/>
	<param name="FlashVars" value="playerID=1&amp;soundFile=${media}"/>
	<param name="quality" value="high"/>
	<param name="menu" value="false"/>
	<param name="wmode" value="transparent"/>
      </object>
      <#-- elseif (mediaRes != "" && (mediaRes.contentType == 'video/mpeg' || mediaRes.contentType == 'video/quicktime'))>
      <object id="videoplayer1" classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" width="320" height="255" codebase="http://www.apple.com/qtactivex/qtplugin.cab">
        <param name="src" value="${media}"/>
        <param name="autoplay" value="false"/>
        <param name="controller" value="true"/>
        <param name="loop" value="false"/>
        <embed id="videoplayer1" src="${media}" width="320" height="255" autoplay="false" controller="true" loop="false" pluginspage="http://www.apple.com/quicktime/download/">
        </embed>
      </object -->
      <#else>
      <a class="vrtx-media" href="${media}"><@vrtx.msg code="article.media-file" /></a>
      </#if>
    </#if>
    <#-- Authors and published date --> 

    <#assign authors = propValue("authors") />
    <#assign published = propValue("published-date") />
    <#if authors != "" || published != "">
      <div class="vrtx-byline">
        <#if authors != "" && published != "">
          <@vrtx.msg code="article.by" /> ${authors?html} <br />${published}
        <#elseif authors != "">
          <@vrtx.msg code="article.by" /> ${authors?html}
        <#elseif published != "">
          ${published}
	</#if>
      </div>
    </#if>

    <#-- Start-date, end-date and location --> 

    <#assign start = propValue("start-date") />
    <#assign startiso8601 = propValue("start-date", resource, "iso-8601") />
    <#assign startshort = propValue("start-date", resource, "short") />
    <#assign end = propValue("end-date") />
    <#assign endiso8601 = propValue("end-date", resource, "iso-8601") />
    <#assign endshort = propValue("end-date", resource, "short") />
    <#assign endhoursminutes = propValue("end-date", resource, "hours-minutes") />
    <#assign location = propValue("location") />

    <#if start != "" || end != "" || location != "">
      <div class="vevent">
        <span class="summary" style="display:none;">${title}</span>
	<#if start != "">
	  <abbr class="dtstart" title="${startiso8601}">${start}</abbr>
	</#if>
	<#if end != "">
	  <#if startshort == endshort>
          - <abbr class="dtend" title="${endiso8601}">${endhoursminutes}</abbr><#else>
	  - <abbr class="dtend" title="${endiso8601}">${end}</abbr></#if><#if location != "">,
	    <span class="location">${location}</span>
	  </#if>
	</#if>
     </div>
    </#if>

    <div class="vrtx-bodytext">
      ${resource.bodyAsString}
    </div>

    <#-- Keywords -->

    <#assign keywords = resource.getValueByName("keywords")?default("") />
    <#assign tagsProp = resource.getPropertyByName("keywords")?default("") />
    <#if keywords != "">
      <div class="vrtx-keywords">
        ${tagsProp.definition.getLocalizedName(springMacroRequestContext.locale)}:
	<#list tagsProp.values as tag>
          <a href="/?vrtx=tags&amp;tag=${tag.stringValue?html}">${tag.stringValue?html}</a>
	</#list>
      </div>
    </#if>

  </body>
</html>


