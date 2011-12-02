<#ftl strip_whitespace=true>
<#--
  - File: feedback.ftl
  - 
  - Description: Displays a link to the feedback service
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if emailLink?exists && emailLink.url?exists>

  <#assign link = emailLink.url />
  
   <!-- begin feedback js -->
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <script type="text/javascript"><!--
    $(function() {
      if (typeof urchinTracker !== "undefined") {
        $(".feedback-yes").click(function(e) {
          $(".vrtx-feedback ul").replaceWith('<p><@vrtx.msg code="feedback.thanks" default="Thank you for giving us feedback" /></p>')
            .css("position", "relative");
            .append("<div class='feedback-thanks-slider'></div>");
            .find(".feedback-thanks-slider").animate({ left: 200 },
                                                     { queue: false,
                                                       duration: 200 });
          });
          
          urchinTrack("/like");
          e.stopPropagation();
          e.preventDefault(); 
        });
        $(".feedback-no").click(function() {
          urchinTrack("/dislike");
        });
      } else {
        var noLink = $(".vrtx-feedback a.feedback-no").parent();
        noLink.find("a").removeClass("feedback-no")
                        .addClass("feedback")
                        .text('<@vrtx.msg code="feedback.link" default="Give feedback" />');
        $(".vrtx-feedback ul").replaceWith('<p>' + noLink.html() + '</p>');
        tb_init("a.feedback");
      }
    });
    function urchinTrack(action) {
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
        <a class="feedback-yes" title='<@vrtx.msg code="feedback.thanks" default="Thank you for giving us feedback" />' href="#">
          <@vrtx.msg code="feedback.link.yes" default="Yes, my questions were answered" />
        </a>
      </li>
      <li>
        <a class="feedback-no thickbox" title='<@vrtx.msg code="feedback.thanks" default="Thank you for giving us feedback" />' href="${link?html}&amp;height=350&amp;width=370&amp;KeepThis=true&amp;TB_iframe=true">
          <@vrtx.msg code="feedback.link.no" default="No, I didn't find what I was looking for" />
        </a>
      </li>
    </ul>
    <span class="vrtx-feedback-bottom"></span>
  </div>
</#if>