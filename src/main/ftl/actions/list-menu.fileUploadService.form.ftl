<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if uploadForm?exists && !uploadForm.done>
<div style="clear:both;height:1px;visibility:hidden;"></div>
<form class="collectionMenu vrtx-admin-form" 
        action="${uploadForm.submitURL?html}" method="post"
        enctype="multipart/form-data">
    <h3><@vrtx.msg code="actions.fileUploadService" default="Upload File"/>:</h3>
    <@spring.bind "uploadForm.file" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	</ul>
      </#if>
    <input type="file" name="file"  multiple>
    <div id="submitButtons">
      <input type="submit" name="save" value="<@vrtx.msg code="actions.fileUploadService.save" default="Save"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.fileUploadService.cancel" default="Cancel"/>">
    </div>      
  </form>
</#if>
<#recover>
${.error}
</#recover>
