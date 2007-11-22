<#ftl strip_whitespace=true>

<#--
  - File: article.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -
  -->

<#import "/spring.ftl" as spring />

<#function propValue propName>
<#local prop = resource.getPropertyByPrefix("", propName)?default("") />
<#if prop != "">
  <#local type = prop.definition.type />
  <#if type = 'DATE' || type = 'TIMESTAMP'>
    <#local locale = springMacroRequestContext.getLocale() />
    <#return prop.getFormattedValue("long", locale) />
  <#else>
    <#return prop.formattedValue />
  </#if>

</#if>
<#return "" />
</#function>

<#function propResource propName>
  <#local prop = resource.getPropertyByPrefix("", propName)?default("") />
  <#if prop != "">
    <#local def = prop.definition />
    <#local type = def.type />
    <#if type = 'IMAGE_REF'>
      <#local tmpResource = resource.getPropResource(def)?default("") />
      <#return tmpResource />
    </#if>
  </#if>
  <#return "" />
</#function>


<#assign title = propValue("userTitle") />
<#if title == "">
  <#assign title = "Missing title" />
</#if>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head><title>${title}</title>

    <style type="text/css">

/*      body {
        font-family: arial,helvetica,"helvetica neue",sans-serif;
        font-size: 75%;
        color: #636363;
      }
      
      p {line-height: 1.4em; margin-bottom: 0.5em;}
      
      h1 {
        color: #2b450f;
	font-size: 26px;
	font-weight: normal;
      }

      h2 {
        font-size: 1em;
        font-weight: bold;
      }

      .byline {
	border-top: 1px dotted rgb(160,175,131);
	font-size: 11px;
	line-height: 1.2em;
      } 

*/

      /* Ingress */

      .introduction {}

      /* Bilde uten bildetekst */

      img.introduction-image {
      float: left;
      margin: 0em 0.75em 0.5em 0;
      border:1px solid #ddd; 
      }

      /* Bilde med bildetekst */

      div.introduction-image {
      border:1px solid #ddd; 
      float: left;
      margin: 0em 0.75em 0.5em 0;
      }

      div.introduction-image img {
      }

      div.introduction-image div.text {
      overflow: hidden;
      padding: 0.5em; 
      background-color: #eee;
      }

      div.introduction-image div.text span.title{
      font-weight: bold;
      }

     /* Body /*  

     .bodyText p {} 
           
    </style>
</head>
  <body>
    <h1>${title}</h1>

    <#assign imageRes = propResource("picture") />
    <#assign introductionImage = propValue("picture") />
    <#if introductionImage != "">
      <#if imageRes == "">
        <img class="introduction-image" src="${introductionImage}" alt="ingressbilde" />
      <#else>

        <#assign userTitle = imageRes.getValueByName("userTitle")?default("") />
        <#assign desc = imageRes.getValueByName("description")?default("") />

	<#if userTitle == "" && desc == "">  
          <img class="introduction-image" src="${introductionImage}" alt="ingressbilde-2" />
	<#else>
          <#assign pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
          <#if pixelWidth != "">
            <#assign style = "width:" + pixelWidth+ "px;" />
          </#if>
	 	 
          <div class="introduction-image" style="${style}">
	    <#if userTitle != "">
	      <img src="${introductionImage}" alt="${userTitle}" />
	    <#else>
	      <img src="${introductionImage}" alt="ingressbilde" />
	    </#if>
            <div class="text">
	      <#if userTitle != "">
		<span class="title">${userTitle}<#if desc != "">: </#if></span>
	      </#if>
	      <#if desc != "">
		<span class="description">${desc}</span>
	      </#if>
	    </div> 
	  </div>
	</#if>
      </#if>
    </#if>

    <#assign introduction = propValue("introduction") />
    <#if introduction != "">
      <div class="introduction">
        ${introduction}
      </div>
    </#if>

    <#assign authors = propValue("authors") />
    <#assign published = propValue("published-date") />
<div class="byline">
    <#if authors != "" && published != "">
      Av ${authors} <br />${published}
    <#elseif authors != "">
      Av ${authors}
    <#elseif published != "">
      ${published}
    </#if>
</div>

    <#assign start = propValue("start-date") />
    <#assign end = propValue("end-date") />
    <#assign location = propValue("location") />

<#if start != "" || end != "" || location != "">
<div class="eventMicroFormat">
  <#if start != "">Starter: ${start}<br/></#if>
  <#if end != "">Slutter: ${end}<br/></#if>
  <#if location != "">Stad: ${location}<br/></#if>
</div>
</#if>

        <div class="bodyText">
          ${resource.bodyAsString}
        </div>


  <#assign mediaRes = propResource("media-ref") />
<#if mediaRes != "" && mediaRes.resourceType == 'audio'>
  <#assign media = propValue("media-ref") />
  <script type="text/javascript" language="JavaScript" src="${mediaPlayerBase.url?html}/audio-player.js"></script>
  <object type="application/x-shockwave-flash" data="${mediaPlayerBase.url?html}/player.swf" id="audioplayer1" height="24" width="290">
    <param name="movie" value="${mediaPlayerBase.url?html}/player.swf"/>
    <param name="FlashVars" value="playerID=1&amp;soundFile=${media}"/>
    <param name="quality" value="high"/>
    <param name="menu" value="false"/>
    <param name="wmode" value="transparent"/>
  </object>
</#if>
  </body>
</html>


