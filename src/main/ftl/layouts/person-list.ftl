<#import "/lib/vortikal.ftl" as vrtx />

<#if relatedPersons?exists && (relatedPersons?size > 0)>
	<@listPersons />
</#if>

<#macro listPersons >
<div class="vrtx-participants">
<#if name != "master-list" >
  <h2><a href="${showAllPersons}"><@vrtx.msg code="${name}.project-participants" /></a></h2>
<#else>
  <h2><@vrtx.msg code="${name}.project-participants" /></h2>
</#if>
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
  <#if name != "master-list" >
  	<a class="all-messages" href="${showAllPersons}"><@vrtx.msg code="decorating.feedComponent.allMessages" /></a>
  </#if>
</div>
</#macro>