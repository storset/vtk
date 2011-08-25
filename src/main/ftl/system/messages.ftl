<#ftl strip_whitespace=true />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign requestContext = resourceContext.requestContext />

<#list requestContext.errorMessages as msg>
  <div class="errormessage ${msg.identifier?html}">
    ${msg.title?html}
    <#if (msg.messages)?exists>
      <ul class="errors">
        <#list msg.messages as subMsg>
          <li>${subMsg?html}</li>
        </#list>
      </ul>
    </#if>
  </div>
</#list>

<#list requestContext.infoMessages as msg>
  <div class="infomessage ${msg.identifier?html}">
    ${msg.title?html}
    <#if msg.messages?has_content>
      <ul class="infoitems">
        <#list msg.messages as subMsg>
          <li>${subMsg?html}</li>
        </#list>
      </ul>
    </#if>
  </div>

</#list>

<#-- XXX: remove -->
<#if expiresSec?exists && expiresSec["expires-sec"]?exists>
  <#assign delay = expiresSec["expires-sec"]?number / 60>
  <#if delay &gt;= 5>
  <div class="infomessage">
    <#assign delay = delay?string("0.###")>
    <@vrtx.msg "headerControl.expiresSec",
    "This resource uses the expires property", [delay] />
  </div>
  </#if>
</#if>
