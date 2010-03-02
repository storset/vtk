$(document).ready( function() {
	
  var wrapper = ".vrtx-image-listing-include";
  var container = ".vrtx-image-listing-include-container";
  var fadeInOutTime = 100; //ms
  
  // Unobtrusive JavaScript
  $(container + "-pure-css").addClass("vrtx-image-listing-include-container");
  
  $(wrapper + " ul li a").each( function(i) {
      $(this).click(function(e) {
    	  
    	  //change image
    	  var img = new Image();
    	  var src = $("img", this).attr("src").split("?")[0]; img.src = src; img.alt = src;
    	  
          //change link
          link = document.createElement("a"); 
          link.setAttribute("href", $(this).attr("href"));
          
          //add them together
          $(link).append(img);
          
      	  //replace link and image (w/ fade effect down to 0.5 opacity)
          $(wrapper + " " + container).animate({opacity: 0.5}, fadeInOutTime, function() {      
        	  $("a", this).remove();
        	  $(this).animate({opacity: 1.0}, fadeInOutTime);
        	  $(this).append(link);
          });
          
          //remove active classes
          jQuery(wrapper + " ul li a").each(function(j) {
        	  jQuery(this).removeClass("active");
	      });    
         
          //add new active class
      	  $(this).addClass("active");
      	  
          //prevent default event action
    	  e.preventDefault(); 
      });
  });
  
});