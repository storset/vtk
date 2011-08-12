<#--
  - File: facebook-comments.ftl
  - 
  - Description: Displays a iframe with comments from facebook.
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<script type="text/javascript"><!--    
    var tries = 0;
    var giveUpAfterTries = 1000; // 1000*10ms = 10s
    
    var facebookIframeReadyTimer = setInterval(function() {
      if($("iframe#fbc").length) {
        var facebookIframe = $("iframe#fbc");
        var inner = facebookIframe.contents();
        if(inner.length) {
          var innerFacebookIframe = inner.find("iframe");
          if(innerFacebookIframe.length) {
            var innerFacebookIframeHeight = innerFacebookIframe.height();
            if(!isNaN(innerFacebookIframeHeight)) {
              facebookIframe.css("height", innerFacebookIframeHeight + "px");
              clearInterval(facebookIframeReadyTimer);
            }
          }
        }
      }
      tries++;
      if(tries == giveUpAfterTries) {
        clearInterval(facebookIframeReadyTimer); 
      }
    }, 10);
// -->
</script>

<iframe allowtransparency="true" frameborder="0" id="fbc" scrolling="no" src="http://folk.uio.no/adrianhj/fc.html?url=${URL}" 
    style="border:none; overflow:hidden; width:100%; height:500px">Your browser does not support iframe.</iframe>