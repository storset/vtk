<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="">
	 <div class="${classes}"> 
        <label for="${inputFieldName}">${title}</label>
        <div>
          <textarea name="${inputFieldName}" id="${inputFieldName}" class="html" rows="4" >${value}</textarea>
        </div>
    </div>
</#macro>