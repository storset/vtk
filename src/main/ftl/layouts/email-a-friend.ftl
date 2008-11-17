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

<script type="text/javascript">
          var newwindow;
          var iMyWidth;
          var iMyHeight;
          function popup(url) {

            //half the screen width minus half the new window width (plus 5 pixel borders).
            iMyWidth = (window.screen.width/2) - (75 + 10);

            //half the screen height minus half the new window height (plus title and status bars).
            iMyHeight = (window.screen.height/2) - (100 + 50);
 
            //Open the window
            var win2 = window.open(url,"Window2","status=no,height=450,width=340,resizable=no,left=" + iMyWidth + ",top=" + iMyHeight + ",screenX=" + iMyWidth + ",screenY=" + iMyHeight + ",toolbar=no,menubar=no,scrollbars=no,location=no,directories=no");
            win2.focus();
          }
</script>

<a class="vrtx-email-friend" href="javascript:popup('${emailLink.url?html}&firstRun=true');"><@vrtx.msg code="decorating.emailAFriendComponent.emaillink" default="E-mail a friend" /></a>