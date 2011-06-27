<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if uploadForm?exists && !uploadForm.done>
<div class="expandedForm vrtx-admin-form">
<form name="fileUploadService" action="${uploadForm.submitURL?html}" method="post" enctype="multipart/form-data">
    <h3><@vrtx.msg code="actions.fileUploadService" default="Upload File"/>:</h3>
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
    <div class="vrtx-textfield">
	  <input id="file" type="file" name="file">
	</div>
    <script type="text/javascript">
    <!--
		$("#file").attr("multiple","multiple");
	-->
	</script>
    <div id="submitButtons">
      <div class="vrtx-button">
        <input type="submit" name="save" value="<@vrtx.msg code="actions.fileUploadService.save" default="Save"/>">
      </div>
      <div class="vrtx-button">
        <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.fileUploadService.cancel" default="Cancel"/>">
      </div>
    </div>      
  </form>
</div>
</#if>
<#recover>
${.error}
</#recover>
