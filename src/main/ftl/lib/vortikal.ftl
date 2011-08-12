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


<#-- XXX: remove this when properties 'introduction' and 'description'
     are merged: -->
<#function getIntroduction resource>
  <#local introduction = vrtx.propValue(resource, "introduction") />
  <#local resourceType = resource.resourceType />
  <#if !introduction?has_content && resourceType != 'collection' 
       && resourceType != 'event-listing' && resourceType != 'article-listing' >
    <#local introduction = vrtx.propValue(resource, "description", "", "content") />
  </#if>
  <#return introduction />
</#function>


<#macro localizeMessage code default args=nullArg locale=springMacroRequestContext.getLocale() >
  
  <#--
    VRTX_MESSAGE_SOURCE is a springframework.*.ResourceBundleMessageSource. It requires
    resourcebundles to be suffixed with the locale used to fetch messages. Therefore, 
    our current solution won't work for nynorsk because we add the contentlocale "*_NY" 
    to these resources and we have no resourcebundle with the suffix "_ny". Hence we 
    hack a fix like so:

    XXX: the mapping  ny:nn should be done in Java code
  -->
  <#if locale?string?contains('NY')>
    <#local constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#local locale = constructor("java.util.Locale", "nn") />
  </#if>
  
  <#local localizedMessage = VRTX_MESSAGE_SOURCE.getMessage(code, args, default, locale) />
  ${localizedMessage?replace("[", "")?replace("]", "")}
</#macro>

<#--
 * msg
 *
 * Get a localized, html-escaped message from the spring RequestContext. 
 * Example: <@vrtx.msg code="my.message.key" default="My default message" args="['my param']"
 *
 * @param code the code of the localized message
 * @param default (optional - set to code if not specified) the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#macro msg code default=code args=[] >
  <#compress>
    <#local localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
    ${localizer.msg?html}
  </#compress>
</#macro>

<#function resourceLanguage>
  <#if resourceLocaleResolver?exists>
     <#local l = resourceLocaleResolver.resolveLocale(null)?string />
     <#return getMsg("language."+l, l) />
  </#if>
</#function>

<#--
 * rawMsg
 *
 * Get a localized, unescaped message from the spring RequestContext. 
 * Example: <@vrtx.rawMsg code="my.message.key" default="My default message" args="['my param']"
 *
 * @param code the code of the localized message
 * @param default the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#macro rawMsg code default args=[] >
  <#compress>
    <#local localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
  ${localizer.msg}
  </#compress>
</#macro>


<#--
 * getMsg
 *
 * Same as the macro 'rawMsg', but returns a value instead of printing it.
 * Example: <#assign msg = vrtx.getMsg("my.message.key", "My default message", ['my param']) />
 *
 * @param code the code of the localized message
 * @param default the default message if the localized message did not
 *        exist for the currently selected locale.
 * @param args (optional) arguments for the message
 *
-->
<#function getMsg code default=code args=[] >
    <#assign localizer =
    "org.vortikal.web.view.freemarker.MessageLocalizer"?new(code, default, args, springMacroRequestContext) />
  <#return localizer.msg/>
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
<#macro formRadioButtons path options pre post cTN=false attributes="">
  <@spring.bind path/>
  <#list options?keys as value>${pre}
  <input type="radio" name="${spring.status.expression}" id="${value}" value="${value}"
    <#if spring.status.value?default("") == value>checked="checked"</#if> <#if cTN>onclick="javascript:changeTemplateName('${options[value]}')"</#if> ${attributes}
  <@spring.closeTag/><label for="${value}">${options[value]}</label>
  ${post}
  </#list>
</#macro>


<#--
 * date
 *
 * Get a localized, formatted date string from a date object.
 * Examples: <@vrtx.date value=my.date.object format='short' />
 *           <@vrtx.date value=my.date.object format='yyyy-MM-dddd HH:mm:ss' />
 *
 * @param value the date object
 * @param format a named format, or a java DateFormat
 *        string. See org.vortikal.repository.resourcetype.ValueFormatter
 *
-->
<#macro date value format>
  <#compress>
    <#if VRTX_DATE_VALUE_FORMATTER?exists>
      <#local constructor = "freemarker.template.utility.ObjectConstructor"?new() />
      <#local val = constructor("org.vortikal.repository.resourcetype.Value", value, false) />
      <#local locale = springMacroRequestContext.getLocale() />
      ${VRTX_DATE_VALUE_FORMATTER.valueToString(val, format, locale)}
    <#else>
      Undefined
    </#if>
  </#compress>
</#macro>

<#function relativeLinkConstructor resourceUri serviceName >
  <#if linkConstructor(resourceUri,serviceName)?exists >
    <#local constructedURL = linkConstructor(resourceUri,serviceName) />
  </#if>
  <#if constructedURL?exists && resourceUri?exists && !resourceUri?contains("://") >
    <#return constructedURL.getPathRepresentation() />
  <#elseif resourceUri?exists >
    <#return resourceUri />
  <#else>
    <#return "" />
  </#if>
</#function>

<#function linkConstructor resourceUri serviceName >
  <#if VRTX_LINK_CONSTRUCTOR?exists && resourceUri?exists && serviceName?exists >
    <#if VRTX_LINK_CONSTRUCTOR.construct(resourceUri,null,serviceName)?exists>
      <#return VRTX_LINK_CONSTRUCTOR.construct(resourceUri,null,serviceName) />
    </#if>
  </#if>
</#function>

<#--
 * calcDate
 *
 * Get a localized, formatted date string from a date object.
 *
 * Same as date macro except it returns the string instead of print it. 
 *
 * Examples: vrtx.date(my.date.object, 'short')
 *           vrtx.date(my.date.object, 'yyyy-MM-dddd HH:mm:ss')
 *
 * @param value the date object
 * @param format a named format, or a java DateFormat
 *        string. See org.vortikal.repository.resourcetype.ValueFormatter
 *
-->
<#function calcDate value format>
  <#if VRTX_DATE_VALUE_FORMATTER?exists>
    <#local constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#local val = constructor("org.vortikal.repository.resourcetype.Value", value, false) />
    <#local locale = springMacroRequestContext.getLocale() />
    <#return VRTX_DATE_VALUE_FORMATTER.valueToString(val, format, locale) />
  <#else>
    <#return "Undefined" />
  </#if>
</#function>

<#--
 * parseInt
 *
 * Attempts to parse an integer from a string
 * Example: <#assign val = vrtx.parseInt("222") />
 *
 * @param value the string value
 *
-->
<#function parseInt value>
  <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
  <#assign parser = constructor("java.lang.Integer", 0) />
  <#return parser.parseInt(value) />
</#function>



<#--
 * flattenHtml
 *
 * Flattens an HTML string.
 * Example: <@vrtx.flattenHtml value='<div>foo</div>' /> (produces 'foo')
 *
 * @param value the HTML string
 * @param escape whether to HTML-escape the flattened string (default true)
 *
-->
<#macro flattenHtml value escape=true>
  <#compress>
    <#if VRTX_HTML_UTIL?exists>
      <#if escape>
        ${VRTX_HTML_UTIL.flatten(value)?html}
      <#else>
        ${VRTX_HTML_UTIL.flatten(value)}
      </#if>
    <#else>
      Undefined
    </#if>
  </#compress>
</#macro>

<#--
 * linkResolveFilter
 *
 * Resolves relative links in an HTML string.
 *
 * @param value the HTML string
 * @param baseURL the URL to the resource that is base for the url
 * @param requestURL URL from the request
 *
-->
<#macro linkResolveFilter value baseURL requestURL >
  <#compress>
    <#if VRTX_HTML_UTIL?exists>
        ${VRTX_HTML_UTIL.linkResolveFilter(value, baseURL, requestURL).getStringRepresentation()}
    <#else>
      Undefined
    </#if>
  </#compress>
</#macro>


<#--
 * invokeComponentRefs
 *
 * Invokes decorating components in an HTML string
 * Example: <@vrtx.invokeComponentRefs html='<div>${include:file uri=[/foo.txt]}</div>' />
 *
 * @param html the HTML string
 *
-->
<#macro invokeComponentRefs html>
  <#local frag = VRTX_HTML_UTIL.parseFragment(html) />
  ${frag.filter(VRTX_DECORATING_NODE_FILTER)}
  ${frag.stringRepresentation}
</#macro>


<#--
 * limit
 *
 * Limits a string to a maximum number of characters
 * Example: <@vrtx.limit nchars='3'>some text</@vrtx.limit> - produces 'som'
 *
 * @param nchars the maximum number of characters
 * @param elide whether or not to append '...' to the string in case
 *        its length is greater than the maximum allowed value
 *
-->
<#macro limit nchars elide=false>
  <#compress>
    <#local val><#nested /></#local>
    <#if val?length &lt; nchars>
      ${val}
    <#else>
      <#local cut_index = nchars />
      <#if cut_index &gt; val?length>
        <#local cut_index = val?length />
      </#if>
      <#local val = val?substring(0, nchars) />
      ${val}
      <#if elide>...</#if>
    </#if>
  </#compress>   
</#macro>

<#--
 * breakSpecificChar
 * Breaks a string if longer than maximum characters on a char
 * Example: <@vrtx.breakSpecificChar nchars=2 char='@'>root@localhost</@vrtx.breakSpecificChar>
 *
 * @param nchars the maximum number of characters before split
 * @param char the char to split on
 *
-->
<#macro breakSpecificChar nchars char>
  <#compress>
    <#local val><#nested /></#local>
    <#if val?length &lt; nchars>
      ${val}
    <#else>
      <#local cut_index = val?index_of(char) />
      <#if (cut_index < 0)>
        <#local cut_index = nchars />
      </#if>
      <#local val = "<span class='comment-author-part-one'>" + val?substring(0, cut_index) + "</span>" 
                  + "<span class='comment-author-part-two'>" + val?substring(cut_index, val?length) + "</span>" />
      ${val}
    </#if>
  </#compress>   
</#macro>

<#--
 * fileNamesAsLimitedList
 * Make filenames with full path into a list with only filename limited by 10
 *
 * @param files the files
 *
-->
<#macro fileNamesAsLimitedList files>
  <#compress>
    <#local maxNumberOfFiles = 10 />
    <#local numberOfRemainingFiles = (files?size - maxNumberOfFiles)  />
    <ul>
    <#local more = false />
    <#list files as file>
      <li>${file?split("/")?last}</li>
      <#if file_index == (maxNumberOfFiles-1)>
        <#local more = true />
        <#break />
      </#if>
    </#list>
    </ul>
    <#if more>
      <p>... <@msg code="trash-can.permanent.delete.confirm.and" default="and"/> ${numberOfRemainingFiles} <@msg code="trash-can.permanent.delete.confirm.more" default="mode"/></p>
    </#if>
  </#compress>
</#macro>


<#--
 * requestLanguage
 *
 * Gets the ISO 639-1 language code for the current request
 *
-->
<#macro requestLanguage>
  <#compress>
    ${springMacroRequestContext.locale.language}
  </#compress>
</#macro>

<#function resourceTypeName resource>
    <#local locale = springMacroRequestContext.getLocale() />
    <#return getMsg("resourcetype.name." + resource.resourceType) />
</#function>

<#function prop resource propName prefix=''>
  <#local prop = getProp(resource, propName, prefix)>
  <#if !prop?has_content>
    <#local prop = getProp(resource, propName, 'resource')>
  </#if>
  <#return prop />
</#function>

<#function getProp resource name prefix=''>
  <#local def = '' />
  <#if VRTX_RESOURCE_TYPE_TREE?exists>
    <#if prefix == "">
      <#if VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(nullArg, name)?exists>
        <#local def = VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(nullArg, name) />
      </#if>
    <#else>
      <#if VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(prefix, name)?exists>
        <#local def = VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(prefix, name) />
      </#if>
    </#if>
  </#if>
  <#if def = ''><#return '' /></#if>
  <#if !resource.getProperty(def)?exists><#return '' /></#if>
  <#return resource.getProperty(def) />
</#function>

<#function propValue resource name format='long' prefix=''>
  <#local propVal = getPropValue(resource, name, format, prefix) />
  <#if !propVal?has_content>
    <#local propVal = getPropValue(resource, name, format, 'resource') />
  </#if>
  <#return propVal />
</#function>

<#function getPropValue resource name format='long' prefix=''>
  <#local def = '' />
  <#if VRTX_RESOURCE_TYPE_TREE?exists>
    <#if prefix == "">
      <#if VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(nullArg, name)?exists>
        <#local def = VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(nullArg, name) />
      </#if>
    <#else>
      <#if VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(prefix, name)??>
        <#local def = VRTX_RESOURCE_TYPE_TREE.getPropertyDefinitionByPrefix(prefix, name) />
      </#if>
    </#if>
  </#if>
  <#local locale = springMacroRequestContext.getLocale() />
  <#if def = ''><#return '' /></#if>
  <#if !resource.getProperty(def)?exists>
    <#local formatter = def.getValueFormatter() />
    <#attempt>
    <#if formatter?exists && !def.mandatory>
      <#return formatter.valueToString(nullArg, format, locale) />
    </#if>
    <#recover></#recover>
    <#return '' />
  </#if>
  <#local prop= resource.getProperty(def) />
  <#local type = prop.definition.type />
  
  <#if type != 'IMAGE_REF'>
    <#return prop.getFormattedValue(format, locale) />
  <#else>
  
    <#-- Hack for relative imagerefs, make sure it doesn't mess up anything else (attempt/recover) -->
    <#attempt>
      <#local imageRef = prop.getStringValue() />
      <#if !imageRef?starts_with("/") && !imageRef?starts_with("http://") && !imageRef?starts_with("https://")>
        <#local imageRef = resource.URI.getParent().expand(imageRef) />
        <#local hackedProp = def.createProperty(imageRef.toString())>
        <#return hackedProp.getFormattedValue(format, locale) />
      </#if>
    <#recover></#recover>
    <#return prop.getFormattedValue(format, locale) />
    
  </#if>
  
</#function>


<#function propResource resource propName prefix="">
  <#local prop = resource.getPropertyByPrefix(prefix, propName)?default("") />
  
  <#if prop != "">
    <#local def = prop.definition />
    <#local type = def.type />
    <#if type = 'IMAGE_REF'>
      <#return resource.getPropResource(def)?default("") />
    </#if>
  </#if>
  <#return "" />
</#function>


<#--
 * csrfPreventionToken
 *
 * Generate a Cross-Site Request Forgery (CSRF) prevention token for a
 * given form URL. Produces a hidden form field with the token.
 *
-->
<#macro csrfPreventionToken url>
  <input type="hidden"
         name="${statics['org.vortikal.security.web.CSRFPreventionHandler'].TOKEN_REQUEST_PARAMETER}"
         value="${VRTX_CSRF_PREVENTION_HANDLER.newToken(url)}" />
</#macro>

<#macro displayTime timeSec >
    <#local sec = (timeSec % 60) />
    <#local min = ((timeSec-sec) % 3600) / 60 />
    <#local hours = ((timeSec-(sec+(min*60))) / 3600 ) />
    <#if (hours > 0)>${hours}:</#if>${min}:<#if (sec > 0 || (hours > 0 || min > 0) )>${sec?string("00")}</#if>
</#macro>

<#macro calculateResourceSize contentLength>
  <#if contentLength <= 1000>
    ${contentLength} B
  <#elseif contentLength <= 1000000>
    ${(contentLength / 1000)?string("0.#")} KB
  <#elseif contentLength <= 1000000000>
    ${(contentLength / 1000000)?string("0.#")} MB
  <#elseif contentLength <= 1000000000000>
    ${(contentLength / 1000000000)?string("0.#")} GB
  <#else>
    ${contentLength} B
  </#if>
</#macro>

<#macro calculateResourceSizeToKB contentLength>
  ${(contentLength / 1000)?string("#")} KB
</#macro>

<#function getMetadata metadata key multiple=false>
  <#if multiple = true>
    <#if metadata.getValues(key)?exists>
      <#return metadata.getValues(key) />
    </#if>
  </#if>
  <#assign value = "" />
  <#if metadata.getValue(key)?exists>
    <#assign value = metadata.getValue(key) />
  </#if>
  <#return value />
</#function>

<#--
 * getEvenlyColumnDistribution
 *
 * Get number of items per column when totalItems is evenly distributed across maxColumns (from left to right)
 *
 * Based on: http://stackoverflow.com/questions/1244338/algorithm-to-evenly-distribute-items-into-3-columns
 * (with ?floor instead of ?round to distribute e.g. 10 as 4,3,3 and not 3,3,4)
 *
-->
<#function getEvenlyColumnDistribution totalItems column maxColumns>
  <#assign n = (totalItems / maxColumns)?floor />
  <#if ((totalItems % maxColumns) >= column)>
    <#assign n = n + 1 />
  </#if>
  <#return n />
</#function>

<#macro displayUserPrincipal principal>
  <#compress>
    <#if principal.URL?exists>
      <a title="${principal.name?html}" href="${principal.URL?html}">${principal.description?html}</a>
    <#else>
      ${principal.name?html}
    </#if>
  </#compress>
</#macro>

<#macro iconResolver resourceType="" contentType="">
  <#compress>
    <#if resourceType = "file">
      <#if contentType = "application/octet-stream">
        binary
      <#else>
        ${resourceType}
      </#if>
    <#else>
      ${resourceType}
    </#if>
  </#compress>
</#macro>

<#function resolveInheritedProperty propertyName>
  <#assign resource = VRTX_INHERITED_PROPERTY_RESOLVER.resolve(propertyName)?default("undefined") />
  <#if resourceContext.currentResource.URI = "/">
    <#local inherited = false />
  <#elseif !resource?exists || resource == "undefined">
    <#local inherited = true />
  <#elseif resource.URI != resourceContext.currentResource.URI>
    <#local inherited = true />
  <#else>
    <#local inherited = false />
  </#if>
  <#if resource == "undefined">
    <#return {"exists":false, "inherited":inherited} />
  <#else>
    <#assign value = propValue(resource, propertyName)?default("") />
    <#assign localizedValue = propValue(resource, propertyName, 'localized')?default("") />
    <#assign prop = getProp(resource, propertyName) />
    <#return {"exists":true, "resource":resource, "property":prop,
             "value":value, "localizedValue":localizedValue, "inherited":inherited} />
  </#if>
</#function>
