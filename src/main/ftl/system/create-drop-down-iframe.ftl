<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery//plugins/jquery.ba-postmessage.min.js"></script> 
<script type="text/javascript">
  $(document).ready(function () {
  $.receiveMessage(function(e) {
    var previewIframe = $("#create-iframe")[0];
    if (previewIframe) {
      var recievedData = e.data;
      if(recievedData.indexOf("fullsize") != -1) {
        previewIframe.style.height = document.body.clientHeight + "px";
        previewIframe.style.width = document.body.clientWidth + "px";
      }
      
    }
  }); // TODO: here we can add where we only want to receive from, e.g. }, "<domain>");
});
</script>

<iframe id="create-iframe" src="${create.url}" allowTransparency="true" height="50px" width="200px">
  [Du har ikke iframe]
</iframe>
