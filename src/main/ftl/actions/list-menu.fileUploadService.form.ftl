<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/actions.ftl" as actionsLib />

<#if uploadForm?exists && !uploadForm.done>
  <div class="expandedForm vrtx-admin-form">
    <form name="fileUploadService" id="fileUploadService-form" action="${uploadForm.submitURL?html}" method="post" enctype="multipart/form-data">
      <h3><@vrtx.msg code="actions.fileUploadService" default="Upload File"/></h3>
      <@spring.bind "uploadForm.file" /> 
      <@actionsLib.genErrorMessages spring.status.errorMessages /> 
      <div id="file-upload-container">   
        <input id="file" type="file" name="file" />
      </div>
      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.fileUploadService.save" "actions.fileUploadService.cancel" />     
    </form>
  </div>
</#if>

<#recover>
${.error}
</#recover>
