<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" name="" baseFolder="/ " inputFieldSize=40>
  <div class="vrtx-resource-ref ${classes}">
	<div class="vrtx-resource-ref-label">
      <label for="${inputFieldName}">${title}</label>
	</div>
	<div class="vrtx-resource-ref-browse">
	  <input class="vrtx-textfield" type="text" id="${inputFieldName}" name="${inputFieldName}" value="${value?html}" size="${inputFieldSize}" />
      <button class="vrtx-button" type="button" onclick="browseServer('${inputFieldName}', '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}','File');"><@vrtx.msg code="editor.browseImages"/></button>
	</div>
  </div>
</#macro>