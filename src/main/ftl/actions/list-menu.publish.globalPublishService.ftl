<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("publish.header") />
<#assign titleMsg = vrtx.getMsg("publish.title") />

<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL + '&showAsHtml=true&height=110&width=250' />
  <#assign method = "get" />
</#if>

<h3>${headerMsg}</h3>
<p><span class="unpublished"><@vrtx.msg code="publishing.unpublish-date" /></span></p>
<form id="vrtx-publish-document" action="${actionURL?html}" method="${method}" class="vrtx-admin-button">
   <div class="vrtx-button-small"><button title="${titleMsg}" type="submit"
            id="vrtx-publish-document.submit"
            value="publish-document" name="action">${item.title?html}</button></div>
</form>

<#recover>
${.error}
</#recover>
