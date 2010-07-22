<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" baseFolder="/">
<div class="vrtx-image-ref ${classes}">
	<div class="vrtx-image-ref-label">
		<label for="${inputFieldName}">${title}</label>
	</div>
	<div class="vrtx-image-ref-browse">
	   <input type="text" id="${inputFieldName}" name="${inputFieldName}" value="${value?html}" onblur="previewImage($(this).parent().find('input').attr('id'));" size="30" />
       <button type="button" onclick="browseServer($(this).parent().find('input').attr('id'), '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
	</div>

	<div id="${inputFieldName}.preview">
		<#if value?has_content >
			<img src="${value?html}" alt=""/>
		</#if>
	</div>

</div>
</#macro>