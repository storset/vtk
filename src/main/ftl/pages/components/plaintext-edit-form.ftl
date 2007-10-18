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
<#assign type=resourceContext.currentResource.resourceType />
<#assign class>class="codepress generic autocomplete-off"</#assign>
<#if type=="xhtml10trans" || type=="html">
  <#assign class>class="codepress html autocomplete-off"</#assign>
<#elseif type=="php">
  <#assign class>class="codepress php autocomplete-off"</#assign>
</#if>

  <script type="text/javascript" language="Javascript"><!--
     function updateContent() {
        if (area && area.toggleEditor) area.toggleEditor();
     }
    // -->
  </script>
  <style type="text/css">
    body.hide-line-numbers {background:white;margin-left:16px;}
  </style>
  <div style="width:99%;">
    <form action="${plaintextEditForm.submitURL}" method="POST">
      <textarea id="area" ${class} style="width:100%;" name="content" rows="20" cols="80">${plaintextEditForm.content?html}</textarea>
      <div style="padding-top:7px;">
        <input type="submit" name="saveAction" onclick="updateContent();" value="<@vrtx.msg code="plaintextEditForm.save" default="Save"/>">
        <input type="submit" name="cancelAction" value="<@vrtx.msg code="plaintextEditForm.cancel" default="Cancel"/>">

        <#if plaintextEditForm.tooltips?exists>
          <#list plaintextEditForm.tooltips as tooltip>
           <div class="contextual-help"><a href="javascript:void(0);" onclick="javascript:open('${tooltip.url?html}', 'componentList', 'width=650,height=450,resizable=yes,right=0,top=0,screenX=0,screenY=0,scrollbars=yes');">
              <@vrtx.msg code=tooltip.messageKey default=tooltip.messageKey/>
            </a>
           </div>
          </#list>
        </#if>

      </div>
    </form>
  </div>
