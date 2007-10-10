<#ftl strip_whitespace=true>

<#--
  * ping
  * 
  * Description: Javascript for "pinging" a resource at
  * regular intervals, keeping the session alive, and possibly
  * performing other server-side tasks. Requires the XMLHttpRequest
  * Javascript object.
  * 
  * @param url the url to access 
  * @param interval the ping interval in seconds. The default value is
  *        600 (10 minutes)
  *
  -->
<#macro ping url interval=600>
  <script language="JavaScript" type="text/javascript"><!--
     var intervalSec = ${interval};
     var req;

     function ping() {
        if (req == null) {
           if (window.XMLHttpRequest) {
              req = new XMLHttpRequest();
           } else if (window.ActiveXObject) {
              req = new ActiveXObject("Microsoft.XMLHTTP");
           }
        }
        if (req != null) {
           req.open('HEAD', '${url}', true);
           req.onreadystatechange = callback;
           req.send(null);
        }
     }

     function callback() {
        if (req != null && req.readyState == 4) {
           if (req.status == 200) {
              setTimeout('ping()', intervalSec * 1000);
           }
        }
     }

     ping();
  // -->
  </script>
</#macro>
