<#ftl strip_whitespace=true>
<#--
  - File: feedback.ftl
  - 
  - Description: Displays a link to the feedback service
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if !emailLink?exists || !emailLink.url?exists>
  <#stop "Missing 'emailLink' entry in model"/>
</#if>

<#assign link = emailLink.url />
  
<!-- begin feedback js -->
<#if jsURLs?exists>
  <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
  </#list>
</#if>
<script type="text/javascript"><!--
  $(function() {
    // if (typeof urchinTracker !== "undefined") {
      $(".feedback-yes").click(function(e) { 
        urchinTrack("/like");
        e.preventPropagation();
        e.preventDefault(); 
      });
      $(".feedback-no").click(function() {
        urchinTrack("/dislike");
      });
    // } else {
      // $(".vrtx-feedback").replaceWith('<p><@vrtx.msg code="feedback.no-urchin" default="Urchin is not available" /></p>');
    // }
  });
  function urchinTrack(action) {
    $(".vrtx-feedback ul").replaceWith('<p><@vrtx.msg code="feedback.thanks" default="Thank you for giving us feedback" /></p>');
    _udn="uio.no";
    urchinTracker(action + document.location.pathname);
  }
// -->
</script>
<!-- end feedback js -->

<div class="vrtx-feedback">
  <span class="vrtx-feedback-title"><@vrtx.msg code="feedback.title" default="Did you find what you were looking for?" /></span>
  <#if mailTo?has_content>
    <#assign link = link + "&mailto=" + mailTo?url('UTF-8') />
  </#if>
  <#if contactUrl?has_content>
    <#assign link = link + "&contacturl=" + contactUrl?url('UTF-8') />
  </#if>
  <ul>
    <li>
      <a class="feedback-yes" title='<@vrtx.msg code="feedback.title" default="Give feedback" />' href="#">
        <@vrtx.msg code="feedback.link.yes" default="Yes I found it" />
      </a>
    </li>
    <li>
      <a class="feedback-no thickbox" title='<@vrtx.msg code="feedback.title" default="Give feedback" />' href="${link?html}&amp;like=false&amp;height=400&amp;width=370">
        <@vrtx.msg code="feedback.link.no" default="No I did not find it" />
      </a>
    </li>
  </ul>
  <span class="vrtx-feedback-bottom"></span>
</div>