$(document).ready( function() {
  // Unobtrusive JavaScript
  $(".vrtx-listing-include-container-pure-css").addClass("vrtx-listing-include-container");
  
  $(".vrtx-image-listing-include ul li a").each( function(i) {
      $(this).click(function(e) { 
    	  
    	  //change image
    	  var img = new Image();
    	  var src = $("img", this).attr("src").split("?")[0]; img.src = src; img.alt = src;
          
          //change link
          link = document.createElement("a"); 
          link.setAttribute("href", $(this).attr("href"))
          
          //replace link and image
          $(".vrtx-image-listing-include .vrtx-listing-include-container a").remove();
          $(".vrtx-image-listing-include .vrtx-listing-include-container").append(link);
          $(".vrtx-image-listing-include .vrtx-listing-include-container a").append(img);
          
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