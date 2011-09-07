<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#assign docUrl = docUrl.url />
<#assign collUrl = collUrl.url />
<#assign upUrl = upUrl.url />

<ul class="manage-create"> 
  <li class="manage-create-drop first">
    <a class="thinbox" title="<@vrtx.msg code="manage.choose-location.document" default="Choose where you would like to create document" />" href="${docUrl?html}">
      <@vrtx.msg code="manage.document" default="Create document" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thinbox" title="<@vrtx.msg code="manage.choose-location.collection" default="Choose where you would like to create folder" />" href="${collUrl?html}">
      <@vrtx.msg code="manage.collection" default="Create folder" />
    </a>
  </li>
  <li class="manage-create-drop">
    <a class="thinbox" title="<@vrtx.msg code="manage.choose-location.upload-file" default="Choose where you would like to upload file" />" href="${upUrl?html}">
      <@vrtx.msg code="manage.upload-file" default="Upload file" />
    </a>
  </li>
</ul> 


<script type="text/javascript"><!--
  $("a.thinbox").click(function() {
    openServerBrowser($(this).attr("href"), 600, 335);
    return false;
  });
// -->
</script>