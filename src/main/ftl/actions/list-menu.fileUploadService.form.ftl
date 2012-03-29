<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if uploadForm?exists && !uploadForm.done>
  <div class="expandedForm vrtx-admin-form">
    <form name="fileUploadService" id="fileUploadService-form" action="${uploadForm.submitURL?html}" method="post" enctype="multipart/form-data">
      <h3><@vrtx.msg code="actions.fileUploadService" default="Upload File"/></h3>
      <@spring.bind "uploadForm.file" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <div class="errorContainer">
          <ul class="errors">
            <#list spring.status.errorMessages as error> 
              <li>${error}</li> 
            </#list>
	      </ul>
	    </div>
      </#if>
      <div id="file-upload-container">   
        <input id="file" type="file" name="file" />
      </div>
      <div id="submitButtons">
        <div class="vrtx-focus-button">
          <input type="submit" name="save" value="<@vrtx.msg code="actions.fileUploadService.save" default="Save"/>" />
        </div>
        <div class="vrtx-button">
          <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.fileUploadService.cancel" default="Cancel"/>" />
        </div>
      </div>      
    </form>
  </div>
</#if>

<#recover>
${.error}
</#recover>
