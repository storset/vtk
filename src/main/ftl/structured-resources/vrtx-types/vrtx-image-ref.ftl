<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" baseFolder="/">
<div class="vrtx-image-ref ${classes}">
	<div>
		<label for="${inputFieldName}">${title}</label>
	</div>
	<div>
	  
	   <input type="text" id="${inputFieldName}" name="${inputFieldName}" value="${value?html}" />
       <button type="button" onclick="browseServer('${inputFieldName}', '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
	</div>
</div>
</#macro>