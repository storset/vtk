<#ftl>
<#--
  - File: upload-status.ftl
  -  
  - Description: served to CKeditor as a response to a file upload
  - request. Invokes a Javascript function to let FCK know the status
  - of the completed operation.
  -  
  - 
  - Required model data:
  -  error - the error number
  -  fileURL - the URL of the file just uploaded
  -  fileName - the name of the file just uploaded
  -  
  - Optional model data:
  -  customMessage - a custom error message
  -
  -->
<script type="text/javascript">
  <!--
    var uploadCompletedFunction;
    if (window.parent.frames['frmUpload'].OnUploadCompleted) {
       uploadCompletedFunction = window.parent.frames['frmUpload'].OnUploadCompleted;
    } else {
       uploadCompletedFunction = window.parent.OnUploadCompleted;
    }
    uploadCompletedFunction(
      ${error}, '${(newFileName?html)?default("")}', 
      '${(fileName?html)?default("")}', '${(customMessage?html)?default("")}'
      );
  //-->
</script>
