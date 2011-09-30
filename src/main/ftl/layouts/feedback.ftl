<#ftl strip_whitespace=true>
<#--
  - File: email-a-friend.ftl
  - 
  - Description: Displays a link to the Email-A-Friend service
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if emailLink?exists && emailLink.url?exists>
  <@genFeedback emailLink.url url />
</#if>

<#macro genFeedback link jsUrl addFullUrl="" addTitle="">
  <!-- begin feedback js -->
  <script type="text/javascript" src="${jsUrl?html}"></script>
  <!-- end feedback js -->

  <div class="vrtx-feedback">
    <span class="vrtx-feedback-title">
      <span class="feedback-title"><@vrtx.msg code="feedback.could-not-find" default="Did you find what you were looking for?" /></span>
      <#if addFullUrl != "">
       <#assign href = link?html + "&amp;query=" + addFullUrl?url('UTF-8') />
        <#if addTitle != "">
          <#assign href = href + "&amp;title=" + addTitle?url('UTF-8') />
        </#if>
        <a class="feedback" href="${href}" onClick="javascript:popup('${href}'); return false">
      <#else>
        <a class="feedback" href="${link?html}" onClick="javascript:popup('${link?html}'); return false">
      </#if>
          <@vrtx.msg code="feedback.title" default="Give feedback" />
        </a>
    </span>
    <span class="vrtx-feedback-bottom"></span>
  </div>
</#macro>