<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("actions.showInWindowsExplorer") />

<script type="text/javascript">
<!--          

 if (vrtxAdmin.isIE5OrHigher && vrtxAdmin.isWin) {
   document.write('<a href="${item.url?html}" target="WindowsExplorer" folder="${item.url?html}" style="behavior:url(#default#AnchorClick)">${titleMsg}<\/a>');
 } else {
   var li = $("li.windowsWebdavExplorerService");
   if(li.prev().is("li")) {
     li.prev().not(".last").addClass("last");
     li.remove();
   } else {
     li.parent().remove();
   }
 }
 // -->
</script>