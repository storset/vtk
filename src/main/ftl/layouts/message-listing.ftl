<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/collections/view-message-listing.ftl" as messageListing />

<div class="vrtx-messages-main-content-wrapper">

  <div class="vrtx-messages-header">
    <h2><#if messageFolder??><a href="${messageFolder.URI}"></#if><#if title??>${title}<#else>${vrtx.getMsg("message-listing.title")}</#if><#if messageFolder??></a></#if>
      <#if editMessageFolder?? && editMessageFolder >
        <a class="vrtx-message-listing-create" href="${vrtx.relativeLinkConstructor("${messageFolder.URI}", 'simpleMessageEditor')}">
          ${vrtx.getMsg("message-listing.new-message")}
        </a>
      </#if>
    </h2>
  </div>
  
  <div class="vrtx-messages">
    <#if messageFolder??>
      <#if messages??>
        <@messageListing.displayMessages messages nullArg true compactView/>
        <#if moreMessages?? && moreMessages>
          <div class="vrtx-more">
            <a href="${messageFolder.URI}">${vrtx.getMsg("message-listing.more")}</a>
          </div>
        </#if>
      <#else>
        <p>${vrtx.getMsg("message-listing.no-messages")} ${messageFolder.URI}</p>
      </#if>
    <#else>
       <p>${vrtx.getMsg("message-listing.no-message-folder")}</p>
    </#if>
  </div>
  
</div>