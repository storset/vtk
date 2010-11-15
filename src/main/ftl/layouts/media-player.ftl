<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 
  - Required model data:
  -   resource
  -
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />

<#if media?exists && contentType?exists >

 <#if media != ""> 
  <div class="vrtx-media-ref">
    <#if contentType == "audio" || contentType == "audio/mpeg" || contentType == "audio/mp3" >
    
      <script type="text/javascript" language="JavaScript" src="${audioFlashPlayerJsURL?html}/"></script>
      
      <object type="application/x-shockwave-flash" data="${audioFlashPlayerFlashURL?html}" id="audioplayer1" height="24" width="290">
        <param name="movie" value="${audioFlashPlayerFlashURL?html}"/>
        <param name="FlashVars" value="playerID=1&amp;soundFile=${media}"/>
        <param name="quality" value="high"/>
        <param name="menu" value="false"/>
        <param name="wmode" value="transparent"/>
      </object>
      
      <a class="vrtx-media" href="${media}"><@vrtx.msg code="article.media-file" /></a>
    
     <#elseif contentType == "video/quicktime" >
    
      <object classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" width="${width}" height="${height}" codebase="http://www.apple.com/qtactivex/qtplugin.cab">
        <param name="src" value="${media}"/>
        <param name="autoplay" value="false"/>
        <param name="controller" value="true"/>
        <param name="loop" value="false"/>
        <param name="scale" value="aspect" />         
        <embed src="${media}" width="${width}" height="${height}" autoplay="false" controller="true" loop="false" scale="aspect" pluginspage="http://www.apple.com/quicktime/download/">
        </embed>
      </object> 
	
	<#elseif contentType == "application/x-shockwave-flash" && extension == "swf">
	
		<OBJECT 
			type="application/x-shockwave-flash"
			data="${media}" 
			width="${width}" 
			height="${height}">
		    <PARAM name="movie" value="${media}" />
		    <PARAM name="FlashVars" value="autoplay=${autoplay}" />
		</OBJECT>
		
	<#elseif (contentType == "application/x-shockwave-flash" && extension == "flv") || contentType == "video/mp4">

      	<object width="${width}" height="${height}"> 
			<param name="movie" value="${strobe?html}"></param>
			<param name="flashvars" value="src=${media}"></param>
			<param name="allowFullScreen" value="true"></param>
			<param name="allowscriptaccess" value="always"></param>	
			<embed src="${strobe?html}"
				type="application/x-shockwave-flash" 
			 	allowscriptaccess="always" 
			 	allowfullscreen="true" 
			 	width="${width}" 
			 	height="${height}" 
			 	flashvars="src=${media}">
		 	</embed>
		</object>
	    			
    <#else>
        <object  
			classid="CLSID:22D6F312-B0F6-11D0-94AB-0080C74C7E95" 
			standby="Loading Microsoft Windows Media Player components..." 
			type="application/x-oleobject" 
			codebase="http://activex.microsoft.com/activex/controls/mplayer/en/nsmp2inf.cab#Version=6,4,7,1112" 
			width="${width}" height="${height}" >
			
			<param name="filename" value="'${media}'" />
			<param name="autoStart" value="${autoplay}" />
			<param name="showControls" value="true" />
			<param name="ShowStatusBar" value="true" />
			<param name="ShowDisplay" value="false" />
				<embed 	src="${media}" 
						type="video/x-msvideo" 
						name="MediaPlayer" 
						autoplay="${autoplay}" 
						showcontrols="1" 
						showdisplay="0" 
						width="${width}" 
						height="${height}" /> 
		</object>
    
      	<a class="vrtx-media" href="${media}"><@vrtx.msg code="article.media-file" /></a>
    </#if>
  </div>
  
  </#if>
</#if>