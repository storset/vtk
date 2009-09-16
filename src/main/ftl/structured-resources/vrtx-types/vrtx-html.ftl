<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="">
	 <div class="${classes}"> 
        <label for="${inputFieldName}">${title}</label>
        <div>
          <textarea name="${inputFieldName}" id="${inputFieldName}" rows="4" cols="20" >${value}</textarea>
        </div>
    </div>
</#macro>