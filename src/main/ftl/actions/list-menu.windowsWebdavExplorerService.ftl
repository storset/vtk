<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("actions.showInWindowsExplorer") />
<#assign tooltipMsg = vrtx.getMsg("actions.showInWindowsExplorer.tooltip") />

<script type="text/javascript"><!-- 
 var agent = navigator.userAgent.toLowerCase();         
 var isWin = ((agent.indexOf("win") != -1) || (agent.indexOf("16bit") != -1));
 
 var li = $("li.windowsWebdavExplorerService");
 
 if ($.browser.msie && $.browser.version >= 5 && isWin) {
   li.html('<a href="${item.url?html}" title="${tooltipMsg}" target="WindowsExplorer" folder="${item.url?html}" style="behavior:url(#default#AnchorClick)">${titleMsg}<\/a>');
 } else {
   if(li.prev().is("li")) {
     li.prev().not(".last").addClass("last");
     li.remove();
   } else {
     li.parent().remove();
   }
 }
// -->
</script>