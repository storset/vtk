<#import "/spring.ftl" as spring />

<#--
 * vortikal.ftl
 *
 * (Will become) a collection of useful FreeMarker macros and functions :)
 *
 * The  "exposeSpringMacroHelpers" property on the spring FreeMarker
 * configuration must be set.
 *
 -->

<#if !springMacroRequestContext?exists>
  <#stop "springMacroRequestContext must be exposed. See the
         'exposeSpringMacroHelpers' property of the FreeMarker
         configuration." />
</#if>

<#--
 * msg
 *
 * Get a localized message from the spring RequestContext. 
 * Example: <@vrtx.msg code="my.message.key" default="My default message" args="['my param']"
 *
 * @param code the code of the localized message
 * @param default the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#macro msg code default args=[] >
  <#compress>
    <#assign localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
  ${localizer.msg?html}
  </#compress>
</#macro>


<#--
 * getMsg
 *
 * Same as the macto 'msg', but returns a value instead of printing it.
 * Example: <#assign msg = vrtx.getMsg("my.message.key", "My default message", ['my param']) />
 *
 * @param code the code of the localized message
 * @param default the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#function getMsg code default args=[] >
    <#assign localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
  <#return localizer.msg?html/>
</#function>



<#-- REWORKED SPRINGS VERSION
 * 
 * formRadioButtons
 *
 * Show radio buttons.
 *
 * @param path the name of the field to bind to
 * @param options a map (value=label) of all the available options
 * @param separator the html tag or other character list that should be used to
 *        separate each option.  Typically '&nbsp;' or '<br>'
 * @param attributes any additional attributes for the element (such as class
 *        or CSS styles or size
-->

<#-- FIXME: Only works for CreateDocument -->
<#macro formRadioButtons path options pre post attributes="">
	<@spring.bind path/>
	<#list options?keys as value>${pre}
	<input type="radio" name="${spring.status.expression}" id="${value}" value="${value}"
	  <#if spring.status.value?default("") == value>checked="checked"</#if> onclick="javascript:changetemplatename('${options[value]}')" ${attributes}
	<@spring.closeTag/><label for="${value}">${options[value]}</label>
	${post}
	</#list>
</#macro>

