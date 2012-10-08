<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/actions.ftl" as actionsLib />

<@actionsLib.copyMove item "copy-resources" />
     
<#recover>
${.error}
</#recover>