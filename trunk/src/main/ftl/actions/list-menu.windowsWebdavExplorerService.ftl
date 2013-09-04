<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("actions.showInWindowsExplorer") />
<#assign tooltipMsg = vrtx.getMsg("actions.showInWindowsExplorer.title") />

<a href="${item.url?html}" title="${tooltipMsg}" target="WindowsExplorer" folder="${item.url?html}" style="behavior:url(#default#AnchorClick)">${titleMsg}</a>

</script>