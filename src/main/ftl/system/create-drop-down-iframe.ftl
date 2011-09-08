<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<script type="text/javascript">
/*
  $(window).load(function() {

  var myWidth = 0, myHeight = 0;
  if(typeof( window.innerWidth ) == 'number') {
    //Non-IE
    myWidth = window.innerWidth;
    myHeight = window.innerHeight;
  } else if(document.documentElement && ( document.documentElement.clientWidth || document.documentElement.clientHeight ) ) {
    //IE 6+ in 'standards compliant mode'
    myWidth = document.documentElement.clientWidth;
    myHeight = document.documentElement.clientHeight;
  }
 
  $("#create-iframe").css({"width": myWidth + "px", "height": myHeight + "px"});
  
  });
  */
</script>

<iframe id="create-iframe" src="${create.url}" allowTransparency="true" height="50px" width="200px">
  [Du har ikke iframe]
</iframe>
