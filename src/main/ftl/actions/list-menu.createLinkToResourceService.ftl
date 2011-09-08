<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

${prepend}<a href="javascript:updateParent('${editField}', '${browseURL}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>
