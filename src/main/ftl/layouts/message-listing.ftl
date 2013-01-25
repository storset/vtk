<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/collections/view-message-listing.ftl" as messageListing />

<div class="vrtx-messages-main-content-wrapper">

  <div class="vrtx-messages-header">
    <#if title??>
      <h2>${title}
        <#if editMessageFolder?exists && editMessageFolder >
          <a id="vrtx-message-listing-create" href="${vrtx.relativeLinkConstructor("${messageFolder.URI}", 'simpleMessageEditor')}">
            ${vrtx.getMsg("message-listing.new-message")}
          </a>
        </#if>
      </h2>
    </#if>
  </div>
  
  <#if messageFolder??>
    <div class="vrtx-messages-list">
      <#if messages??>
        <@messageListing.displayMessages messages nullArg compactView/>
        <#if moreMessages?? && moreMessages>
          <a href="${messageFolder.URI}"><@vrtx.msg code="" default="See all messages"/></a>
        </#if>
      <#else>
        <@vrtx.msg code="" default="No messages were found in ${messageFolder.URI}" />
      </#if>
    </div>
  <#else>
     <@vrtx.msg code="" default="No message folder was found" />
  </#if>

</div>