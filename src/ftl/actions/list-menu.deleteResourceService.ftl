<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign msg = vrtx.getMsg("collectionListing.confirmation.delete", "Are you sure you want to delete" + resourceContext.currentResource.name, [resourceContext.currentResource.name]) />
${prepend}<a href="${item.url?html}" onclick="return confirm('${msg}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>
