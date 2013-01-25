<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/collections/view-message-listing.ftl" as messageListing />

<div class="vrtx-messages-main-content-wrapper">

  <div class="vrtx-messages-header">
    <#if title??>
      <h2>${title}
        <#if editMessageFolder?exists && editMessageFolder >
          <a class="vrtx-message-listing-create" href="${vrtx.relativeLinkConstructor("${messageFolder.URI}", 'simpleMessageEditor')}">
            ${vrtx.getMsg("message-listing.new-message")}
          </a>
        </#if>
      </h2>
    <#else>  
      <h2>${vrtx.getMsg("message-listing.title")}</h2>
    </#if>
  </div>
  
  <div class="vrtx-messages">
    <#if messageFolder??>
      <#if messages??>
        <@messageListing.displayMessages messages nullArg compactView/>
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