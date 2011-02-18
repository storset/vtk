<#import "/lib/vortikal.ftl" as vrtx />

<#if relatedPersons?exists && (relatedPersons?size > 0)>
	<@listPersons />
</#if>

<#macro listPersons >
<div class="${name}-participants">
  <h2><a href="${showAllPersons}"><@vrtx.msg code="${name}.project-participants" /></a></h2>
  <ul>
  	<#local i = 0 />
    <#list relatedPersons as person>
      <#local i = i + 1 />
	  <#if person.url??>
		<li><a href="${person.url?html}">${person.name?html}</a></li>
	  <#else>
		<li>${person.name?html}</li>
	  </#if>
	  <#if i == numberOfParticipantsToDisplay >
	  	<#break />
	  </#if>
    </#list>
  </ul>
  <a class="all-messages" href="${showAllPersons}"><@vrtx.msg code="decorating.feedComponent.allMessages" /></a>
</div>
</#macro>