<#ftl strip_whitespace=true>

<#--
  - File: media-refs.ftl
  - 
  - Description: Article media references
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#assign mediaRes = vrtx.propResource(resourceContext.currentResource, "media") />
<#assign media = vrtx.propValue(resourceContext.currentResource, "media") />

<#if media != ""> 
  <div class="vrtx-media-ref">
    <#if mediaRes != "" && mediaRes.resourceType == 'audio'>
      <script type="text/javascript" language="JavaScript" src="${flashPlayer.jsURL?html}/"></script>
      <object type="application/x-shockwave-flash" data="${flashPlayer.flashURL?html}" id="audioplayer1" height="24" width="290">
        <param name="movie" value="${flashPlayer.flashURL?html}"/>
        <param name="FlashVars" value="playerID=1&amp;soundFile=${media}"/>
        <param name="quality" value="high"/>
        <param name="menu" value="false"/>
        <param name="wmode" value="transparent"/>
      </object>
      <#elseif (mediaRes != "" && mediaRes.contentType == 'video/quicktime')>
      <object id="videoplayer1" classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" width="320" height="255" codebase="http://www.apple.com/qtactivex/qtplugin.cab">
        <param name="src" value="${media}"/>
        <param name="autoplay" value="false"/>
        <param name="controller" value="true"/>
        <param name="loop" value="false"/>
        <param name="scale" value="aspect" />         
        <embed id="videoplayer1" src="${media}" width="320" height="255" autoplay="false" controller="true" loop="false" scale="aspect" pluginspage="http://www.apple.com/quicktime/download/">
        </embed>
      </object>
    <#else>
      <a class="vrtx-media" href="${media}"><@vrtx.msg code="article.media-file" /></a>
    </#if>
  </div>
</#if>
