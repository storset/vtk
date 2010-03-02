$(document).ready( function() {
  // Unobtrusive JavaScript
  $(".vrtx-listing-include-container-pure-css").addClass("vrtx-listing-include-container");
  
  $(".vrtx-image-listing-include ul li a").each( function(i) {
      $(this).click(function(e) { 
    	  
    	  //change image
    	  var i = new Image();
    	  var src = $("img", this).attr("src").split("?")[0]; i.src = src; i.alt = src;
          $(".vrtx-image-listing-include .vrtx-listing-include-container img").remove();
          $(".vrtx-image-listing-include .vrtx-listing-include-container").append(i);
          
          jQuery(".vrtx-image-listing-include ul li a").each( function(j) {
            jQuery(this).removeClass("active");
          });
          
          //add new active class
      	  $(this).addClass("active");
          
      	  //prevent default event action
    	  e.preventDefault();
      });
  });
  
});