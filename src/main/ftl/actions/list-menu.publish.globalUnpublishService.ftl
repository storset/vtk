<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("unpublish.header") />
<#assign titleMsg = vrtx.getMsg("unpublish.title") />

<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL + '&showAsHtml=true&height=110&width=250' />
  <#assign method = "get" />
</#if>

<h3>${headerMsg}</h3>
<p><span class="published"><@vrtx.msg code="publishing.publish-date" /></span></p>
<form id="vrtx-unpublish-document" action="${actionURL?html}" method="${method}" class="vrtx-admin-button">
   <div class="vrtx-button"><button title="${titleMsg}" type="submit"
            id="vrtx-unpublish-document.submit"
            value="upublish-document" name="action">${item.title?html}</button></div>
</form>

<#recover>
${.error}
</#recover>
