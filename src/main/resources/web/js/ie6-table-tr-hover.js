// Adds table-tr hover support to IE6 browsers

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
