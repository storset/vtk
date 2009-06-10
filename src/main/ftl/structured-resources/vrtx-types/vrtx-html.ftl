<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="">
	 <div class="vrtx-html ${classes}"> 
        <label for="${inputFieldName}">${title}</label>
        <div>
          <textarea name="${inputFieldName}" id="${inputFieldName}">${value}</textarea>
        </div>
    </div>
</#macro>