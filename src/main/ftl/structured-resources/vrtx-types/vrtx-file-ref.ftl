<#macro vrtxFileRef title tooltip classes>
  <div class="vrtx-file-ref ${classes}">
	<div>
      <label for="${inputFieldName}">${title}</label>
	</div>
	<div class="vrtx-textfield">
	  <input type="file" id="${inputFieldName}" />
	</div>
  </div>
</#macro>