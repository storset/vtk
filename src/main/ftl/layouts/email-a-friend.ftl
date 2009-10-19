<#--
  - File: email-a-friend.ftl
  - 
  - Description: Displays a link to the Email-A-Friend service
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if !emailLink?exists || !emailLink.url?exists>
  <#stop "Missing 'emailLink' entry in model"/>
</#if>

<!-- begin email a friend js -->
<script type="text/javascript">
          var newwindow;
          var iMyWidth;
          var iMyHeight;
          function popup(url) {

            //half the screen width minus half the new window width (plus 5 pixel borders).
            iMyWidth = (window.screen.width/2) - (165 + 10);

            //half the screen height minus half the new window height (plus title and status bars).
            iMyHeight = (window.screen.height/2) - (235 + 50);
 
            //Open the window
            var win2 = window.open(url,"Window2","status=no,height=470,width=330,resizable=no,left=" + iMyWidth + ",top=" + iMyHeight + ",screenX=" + iMyWidth + ",screenY=" + iMyHeight + ",toolbar=no,menubar=no,scrollbars=no,location=no,directories=no");
            win2.focus();
          }
</script>
<!-- end email a friend js -->

<a class="vrtx-email-friend" href="${emailLink.url?html}" onClick="javascript:popup('${emailLink.url?html}'); return false"><@vrtx.msg code="decorating.emailAFriendComponent.emaillink" default="E-mail a friend" /></a>