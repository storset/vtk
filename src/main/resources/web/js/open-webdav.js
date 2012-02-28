$(function() {
  if(typeof agentWebDav === "undefined") {
    var agentWebDav = navigator.userAgent.toLowerCase();         
    var isWinWebDav = ((agentWebDav.indexOf("win") != -1) || (agentWebDav.indexOf("16bit") != -1));
    if ($.browser.msie && $.browser.version >= 7 && isWinWebDav) {  
      $(".vrtx-resource-open-webdav").click(function(e) {
        var openOffice = new ActiveXObject("Sharepoint.OpenDocuments.1").EditDocument(this.href);
        e.stopPropagation();
        e.preventDefault();
      });
      $(".vrtx-resource").hover(function (e) { 
        $(this).find(".vrtx-resource-open-webdav").css("left", ($(this).find(".vrtx-title-link").width() + 63) + "px").show(0);
      }, function (e) {
        $(this).find(".vrtx-resource-open-webdav").hide(0).css("left", "0px");
      });
     $(".vrtx-collection-listing-table tr").hover(function (e) { 
        $(this).find(".vrtx-resource-open-webdav").show(0);
      }, function (e) {
        $(this).find(".vrtx-resource-open-webdav").hide(0);
      });
    }
  }
});