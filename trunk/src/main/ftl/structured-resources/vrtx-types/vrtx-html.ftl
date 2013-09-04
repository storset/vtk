<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="" editor="">
  <div class="${classes}"> 
    <label for="${inputFieldName}">${title}</label>
    <div>
      <textarea class="${editor}" name="${inputFieldName}" id="${inputFieldName}" rows="7" cols="60" >${value?html}</textarea>
    </div>
  </div>
</#macro>