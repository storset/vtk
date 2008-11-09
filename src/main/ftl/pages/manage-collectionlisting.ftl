<#ftl strip_whitespace=true>

<#--
  - File: manage-collectionlisting.ftl
  - 
  - Description: A HTML page that displays a collection listing.
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/lib/collectionlisting.ftl" as col />
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Manage: collection listing</title>
  
  <!-- Adds hover support to IE6 browsers -->
  <script type="text/javascript">
  //<![CDATA[
  
  $.extend({
  
    ie6CSSHover:function(){
      if($.browser.msie && /6.0/.test(navigator.userAgent)){
        var len=document.styleSheets.length;
        for(z=0;z<len;z++){
          var sheet=document.styleSheets[z];
          var css =sheet.cssText;
          var r=new RegExp(/[a-zA-Z0-9\.-_].*:hover\s?\{.[^\}]*\}/gi);
          var m=css.match(r);
          if(m!=null && m.length>0){
            for(i=0;i<m.length;i++){
              var c=m[i].match(/\{(.[^\}]*)}/);
              if(c[1]){
                var seljq=m[i].replace(':hover','').replace(c[0],'');
                var selcss=m[i].replace(':hover','.hover').replace(c[0],'');
                var rule=c[1].replace(/^\s|\t|\s$|\r|\n/g,'');
                document.styleSheets[z].addRule(selcss,rule);
                var grp=$(seljq);
                $(seljq).hover(function(){$(this).addClass('hover')},function(){$(this).removeClass('hover')});
              }
            }
          }
        }
      }
    }
  
  })(jQuery);
  
  $(document).ready(function(){
    $.ie6CSSHover();
  });
  
  //]]>
  </script>
  
</head>
<body>
  <@col.listCollection withForm=true />
</body>
</html>
