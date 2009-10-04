<#ftl strip_whitespace=true>
<#--
  - File: share-at.ftl
  - 
  - Description: Share document on social websites
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#assign resource = resourceContext.currentResource />
<#assign currentURI = resourceContext.currentURI />
<#assign repositoryId = resourceContext.repositoryId />

<#assign title = vrtx.propValue(resource, "title", "") />
<#assign introduction = vrtx.propValue(resource, "introduction", "", "resource") />

<#if introduction?string?length &gt; 250>
  <#assign introduction = introduction?substring(0, 250) + "..." />  
</#if>

<#assign description>
   <@vrtx.flattenHtml value=introduction escape=false />
</#assign>

<a href="#share" id="share-link" class="vrtx-share-link" onclick="share();" name="share"><@vrtx.msg code="decorating.shareAtComponent.title" default="Share at" />...</a>
<div id="vrtx-share-component">
  <div id="send-share">
    <div class="send-inner">
      <div class="share-top">
        <h3><@vrtx.msg code="decorating.shareAtComponent.title" default="Share at" />...</h3>
        <span><a href="#share" class="close-toolbox-send-share" onclick="share();"><@vrtx.msg code="decorating.shareAtComponent.close" default="Close" /></a></span>
      </div>
      <ul>
        <li><a href="http://www.facebook.com/share.php?u=http://${repositoryId}${currentURI}" class="facebook">Facebook</a></li>
        <li><a class="nettby" href="http://www.nettby.no/user/edit_link.php?name=${title}&amp;url=http://${repositoryId}${currentURI}&amp;description=${description}">Nettby</a></li>
        <li><a id="uri" href="http://del.icio.us/post?url=http://${repositoryId}${currentURI}&amp;title=${title}" class="delicious">del.icio.us</a></li>
     </ul>
   </div>
  </div>
</div>