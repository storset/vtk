<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/collections/view-message-listing.ftl" as messageListing />

<div id="vrtx-messages-main-content-wrapper">

<div class="vrtx-messages-header">
<#if title??>
  <h2>${title}</h2>
</#if>
<#if editMessageFolder?exists && editMessageFolder >
  <div class="vrtx-introduction">
    <a id="vrtx-message-listing-create" class="button" href="${vrtx.relativeLinkConstructor("${messageFolder.URI}", 'simpleMessageEditor')}">
      <span>${vrtx.getMsg("message-listing.new-message")}</span>
    </a>
  </div>
</#if>
</div>

<div class="vrtx-messages-list">
<#if messages??>
  <@messageListing.displayMessages messages />
  <#if moreMessages?? && moreMessages>
    <a href="${messageFolder.URI}"><@vrtx.msg code="" default="See all messages"/></a>
  </#if>
<#else>
  <@vrtx.msg code="" default="No messages were found in ${messageFolder.URI}" />
</#if>
</div>

</div>