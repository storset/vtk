<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" name="" baseFolder="/">
<div class="vrtx-url ${classes}">
	<div class="vrtx-url-label">
		<label for="${inputFieldName}">${title}</label>
	</div>
	<div class="vrtx-url-browse">
	   <input type="text" id="${inputFieldName}" name="${inputFieldName}" value="${name?html}" size="20" />
       <button type="button" onclick="browseServer($(this).parent().find('input').attr('id'), '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
	</div>
	
</div>
</#macro>