<#ftl strip_whitespace=true />

<#if systemInfoMessage?exists && systemInfoMessage?has_content>
  <div class="infomessage">${systemInfoMessage}</div>
</#if>
