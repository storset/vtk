<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<script type="text/javascript"><!--
  $(document).ready(function () {
    if (typeof hasReceiveMessageHandler === "undefined" || !hasReceiveMessageHandler) { // not handler in iframe-view.js available
      $.receiveMessage(function(e) {
        var previewIframe = $("#create-iframe")[0];
        if (previewIframe) {
          var recievedData = e.data;
          // Fullsize
          if(recievedData.indexOf && recievedData.indexOf("fullsize") != -1) {
            previewIframe.style.height = document.body.clientHeight + "px";
            previewIframe.style.width = document.body.clientWidth + "px";
          }
          // Back to normal again
          if(recievedData.indexOf && recievedData.indexOf("originalsize") != -1) {
            previewIframe.style.height = 50 + "px";
            previewIframe.style.width = 200 + "px";
          }      
        }
      }); // TODO: here we can add where we only want to receive from, e.g. }, "<domain>");
    }
  });
// -->
</script>

<iframe id="create-iframe" src="${create.url}" allowTransparency="true" height="50px" width="200px">
  [Du har ikke iframe]
</iframe>
