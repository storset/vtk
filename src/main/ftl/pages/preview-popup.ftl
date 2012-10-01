<#ftl strip_whitespace=true>

<#--
  - File: preview-popup.ftl
  - 
  - Description: Display a link to a popup window with a message
  - explaining the reason(s) why the resource cannot be previewed in
  - an iframe (would likely generate a mixed-content warning in the browser).
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>${vrtx.getMsg('preview.popup.title')};</title>
  </head>
  <body>
    <h2>${vrtx.getMsg('preview.sslMixedContent.title')}</h2>
    <#if workingCopy?exists>
      <#if resourceReference?index_of("?") &gt; 0><#assign resourceReference = resourceReference + "&amp;revision=WORKING_COPY" />
        <#assign resourceReference = resourceReference + "&amp;revision=WORKING_COPY" />
      <#else>
        <#assign resourceReference = resourceReference + "?revision=WORKING_COPY" />
      </#if>
    </#if>

    <p>${vrtx.getMsg('preview.sslMixedContent.desc')}</p>
    <a class="vrtx-button" href="${resourceReference?html}" target="vrtx_preview_popup"><span>${vrtx.getMsg('preview.sslMixedContent.open')}</span></a>
    <!--
    <p class="previewUnavailableReasons">${vrtx.getMsg('preview.sslMixedContent.reasons.desc')}</p>
    <#assign prop = vrtx.getProp(resourceContext.currentResource, 'sslMixedMode') />
    <#list prop.values as v>
      <#assign val = v?string />

      <#if val?starts_with("img:")>
        <li><@vrtx.msg code="preview.sslMixedContent.img"  args=[val?substring("img:"?length, val?length)] /></li>

      <#elseif val?starts_with("iframe:")>
        <li><@vrtx.msg code="preview.sslMixedContent.iframe"  args=[val?substring("iframe:"?length, val?length)] /></li>

      <#elseif val?starts_with("frame:")>
        <li><@vrtx.msg code="preview.sslMixedContent.frame"  args=[val?substring("frame:"?length, val?length)] /></li>

      <#elseif val?starts_with("embed:")>
        <li><@vrtx.msg code="preview.sslMixedContent.embed"  args=[val?substring("embed:"?length, val?length)] /></li>

      <#elseif val?starts_with("link:")>
        <li><@vrtx.msg code="preview.sslMixedContent.link"  args=[val?substring("link:"?length, val?length)] /></li>

      <#elseif val?starts_with("base:")>
        <li><@vrtx.msg code="preview.sslMixedContent.base"  args=[val?substring("base:"?length, val?length)] /></li>

      <#elseif val?starts_with("object:")>
        <li><@vrtx.msg code="preview.sslMixedContent.object"  args=[val?substring("object:"?length, val?length)] /></li>

      <#elseif val?starts_with("applet:")>
        <li><@vrtx.msg code="preview.sslMixedContent.applet"  args=[val?substring("applet:"?length, val?length)] /></li>

      <#elseif val?starts_with("property:")>
        <li><@vrtx.msg code="preview.sslMixedContent.property"  args=[val?substring("property:"?length, val?length)] /></li>

      <#elseif val?starts_with("xml:img:")>
        <li><@vrtx.msg code="preview.sslMixedContent.img"  args=[val?substring("xml:img:"?length, val?length)] /></li>

      <#elseif val?starts_with("element:ssi:include:feed")>
        <li><@vrtx.msg code="preview.sslMixedContent.feed"  args=[val?substring("element:ssi:include:feed"?length, val?length)] /></li>

      <#elseif val?starts_with("element:style")>
        <li><@vrtx.msg code="preview.sslMixedContent.style" /></li>

      <#elseif val?starts_with("element:script")>
        <li><@vrtx.msg code="preview.sslMixedContent.script" /></li>

      <#elseif val?starts_with("element:esi:include")>
        <li><@vrtx.msg code="preview.sslMixedContent.esi"  args=[val?substring("element:esi:include"?length, val?length)] /></li>

      <#elseif val?starts_with("attr:on")>
        <li><@vrtx.msg code="preview.sslMixedContent.jsattr"  args=[val?substring("attr:"?length, val?length)] /></li>

      <#elseif val?starts_with("attr:style:")>
        <li><@vrtx.msg code="preview.sslMixedContent.styleattr"  args=[val?substring("attr:style:"?length, val?length)] /></li>
      <#else>
        <li>${v?html}</li>
      </#if>
    </#list>
    </ul>
    -->
  </body>
</html>
