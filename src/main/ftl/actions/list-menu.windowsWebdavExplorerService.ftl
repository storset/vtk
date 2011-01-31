<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("actions.showInWindowsExplorer") />

<script type="text/javascript">
<!--          
 if (is_ie5up && is_win) 
 {
 document.write('(&nbsp;<a href="${item.url?html}" target="WindowsExplorer" folder="${item.url?html}" style="behavior:url(#default#AnchorClick)">${titleMsg}<\/a>&nbsp;)&nbsp;');
 }
 // -->
</script>
<!-- Need this next span to avoid a strange Firefox 1.5 CSS regression -->
<span style="visibility:hidden">foo</span>