<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<ul class="manage-create"> 
  <li class="manage-create-drop first">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.document" default="Choose where you would like to create document" />" href="${docUrl.url?html}">
      <@vrtx.msg code="manage.document" default="Create document" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.collection" default="Choose where you would like to create folder" />" href="${collUrl.url?html}">
      <@vrtx.msg code="manage.collection" default="Create folder" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thickbox" title="<@vrtx.msg code="manage.choose-location.upload-file" default="Choose where you would like to upload file" />" href="${upUrl.url?html}">
      <@vrtx.msg code="manage.upload-file" default="Upload file" />
    </a>
  </li>
</ul>