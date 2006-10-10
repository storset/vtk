<#--
  - File: plaintext-edit-form.ftl
  - 
  - Description: A <div> containing a form for editing plain-text
  - resources
  - 
  - Required model data:
  -   plaintextEditForm
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#if !plaintextEditForm?exists>
  <#stop "Unable to render model: required submodel
  'plaintextEditForm' missing">
</#if>
  <!-- p>Note: The resource is currently locked for five minutes 
  and will be relocked for five minutes every time you save your changes. 
  This prevents others editing at the same time as you.</p -->
  <div style="width:99%;">
    <form action="${plaintextEditForm.submitURL}" method="POST">
      <textarea style="width:100%;" name="content" rows="20" cols="80">${plaintextEditForm.content?html}</textarea>
      <div style="padding-top:7px;">
        <input type="submit" name="saveAction" value="<@vrtx.msg code="plaintextEditForm.save" default="Save"/>">
        <input type="submit" name="cancelAction" value="<@vrtx.msg code="plaintextEditForm.cancel" default="Cancel"/>">
      </div>
    </form>
  </div>

