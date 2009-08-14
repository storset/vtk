<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" baseFolder="/">
<div class="vrtx-image-ref ${classes}">
	<div>
		<label for="${inputFieldName}">${title}</label>
	</div>
	<div id="${inputFieldName}.browse">
	   <input type="text" id="${inputFieldName}" name="${inputFieldName}" value="${value?html}" onblur="previewImage(${inputFieldName});" size="30" />
       <button type="button" onclick="browseServer('${inputFieldName}', '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
	</div>
	<div id="${inputFieldName}.preview">
		<img src="${value?html}?vrtx=thumbnail" />
	</div>
</div>
</#macro>