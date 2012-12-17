<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName tooltip="" classes="" value="" name="" baseFolder="/">
  <div class="vrtx-image-ref ${classes}">
	<div class="vrtx-image-ref-label">
      <label for="${inputFieldName}">${title}</label>
	</div>
	<div class="vrtx-image-ref-browse">
	  <div class="vrtx-textfield">
	    <input type="text" id="${inputFieldName}" name="${inputFieldName}" value="${name?html}" onblur="previewImage($(this).attr('id'));" size="30" />
	  </div>
	  <div class="vrtx-button">
        <button type="button" onclick="browseServer('${inputFieldName}', '${fckeditorBase.url?html}', '${baseFolder}','${fckBrowse.url.pathRepresentation}');"><@vrtx.msg code="editor.browseImages"/></button>
      </div>
	</div>
	<div id="${inputFieldName}.preview" class="vrtx-image-ref-preview<#if !value?has_content> no-preview</#if>">
	  <label for="${inputFieldName}.preview"><@vrtx.msg code="editor.image.preview-title"/></label>
	  <span><@vrtx.msg code="editor.image.no-preview-text"/></span>
	  <div id="${inputFieldName}.preview-inner" class="vrtx-image-ref-preview-inner">
	  <#if value?has_content >
	    <img src="${value?html}" alt="preview" />
	  <#else>
	    <img src="/vrtx/__vrtx/static-resources/themes/default/images/no-preview-image.png" alt="no preview"/>
	  </#if>
	  </div>
	</div>
  </div>
</#macro>