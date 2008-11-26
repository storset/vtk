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
<#import "/lib/view-utils.ftl" as viewutils />


<#assign title = vrtx.propValue(resource, "userTitle" , "flattened") />
<#assign h1 = vrtx.propValue(resource, "userTitle") />

<#if title == "">
  <#assign title = vrtx.getMsg("article.missingTitle") />
  <#assign h1 = title />
</#if>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${title}</title>

    <#-- Comments -->
    <#include "/commenting/comments-feed-url.ftl" />

    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
        <link rel="stylesheet" type="text/css" href="${cssURL?html}" />
      </#list>
    </#if>

  </head>
  <body>
    <h1>${h1}</h1>

    <#-- Image --> 
    <@viewutils.displayImage resource />

    <#-- Introduction -->
    <@viewutils.displayIntroduction resource />

    <#-- Media ref -->

    <#assign mediaRes = vrtx.propResource(resource, "media") />
    <#assign media = vrtx.propValue(resource, "media") />

    <#if media != ""> 
      <#if mediaRes != "" && mediaRes.resourceType == 'audio'>
      <script type="text/javascript" language="JavaScript" src="${flashPlayer.jsURL?html}/"></script>
      <object type="application/x-shockwave-flash" data="${flashPlayer.flashURL?html}" id="audioplayer1" height="24" width="290">
	<param name="movie" value="${flashPlayer.flashURL?html}"/>
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

    <#assign authors = vrtx.propValue(resource, "authors", "enumerated") />
    <#assign published = vrtx.propValue(resource, "published-date") />
    <#if authors != "" || published != "">
      <!-- hr class="vrtx-byline"/ -->

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

    <#assign start = vrtx.propValue(resource, "start-date") />
    <#assign end = vrtx.propValue(resource, "end-date") />
    <#assign location = vrtx.propValue(resource, "location") />

    <#if start != "" || end != "" || location != "">
      <div class="vevent">
        <#t /><@viewutils.displayTimeAndPlace resource title/>
      </div> 
    </#if>

    <div class="vrtx-bodytext">
      ${resource.bodyAsString}
    </div>

    <#-- Tags -->

    <#assign tags = resource.getValueByName("tags")?default("") />
    <#assign tagsProp = resource.getPropertyByName("tags")?default("") />
    <#if tags != "">
      <div class="vrtx-tags">
        ${tagsProp.definition.getLocalizedName(springMacroRequestContext.locale)}:
	<#list tagsProp.values as tag>
          <a href="${tagServiceDefaultExpression?replace("%v", tag.stringValue?html)}">${tag.stringValue?html}</a><#if tag_has_next>,</#if> 
	</#list>
      </div>
    </#if>

    <#-- Comments -->
    <#include "/commenting/comments-component.ftl" />
  </body>
</html>


