<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" name="" baseFolder="/">
  <div class="vrtx-image-ref ${classes}">
	<div class="vrtx-image-ref-label">
      <label for="${inputFieldName}">${title}</label>
	</div>
	<div class="vrtx-image-ref-browse">
	  <div class="vrtx-textfield">
	    <input type="text" id="${inputFieldName}" name="${inputFieldName}" value="${name?html}" onblur="previewImage($(this).parent().find('input').attr('id'));" size="30" />
	  </div>
	  <div class="vrtx-button">
        <button type="button" onclick="browseServer('${inputFieldName}', '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
      </div>
	</div>
	<div id="${inputFieldName}.preview">
	  <#if value?has_content >
	    <img src="${value?html}" alt=""/>
	  </#if>
	</div>
  </div>
</#macro>