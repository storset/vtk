// Vortex Simple Gallery v0.1b
// w/ paging and fade effect
// by UiO / USIT

$(document).ready( function() {
	
  var wrapper = ".vrtx-image-listing-include";
  var container = ".vrtx-image-listing-include-container";
  var fadeInOutTime = 125; //ms
  var fadedOutOpacity = 0.5;
  
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
          link.setAttribute("class", "vrtx-image-listing-include-container-link");
          
          //add them together
          $(link).append(img);
          
      	  //replace link and image (w/ fade effect down to fadedOutOpacity)
          $(wrapper + " " + container).animate({opacity: fadedOutOpacity}, fadeInOutTime, function() {
        	  //done fade out -> remove
        	  $("a.vrtx-image-listing-include-container-link", this).remove();
        	  //start fading in ...
        	  $(this).animate({opacity: 1.0}, fadeInOutTime);
        	  //... before adding new image for smoother change
        	  $(this).prepend(link);
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
  
  //choose first image in <li>
  $(wrapper + " ul li:first a").click();
  
  //paging (relative to li a.active)
  $(wrapper + " " + container + " " + " a.prev").click(function(g) {
	  if($(wrapper + " ul li a.active").parent().prev().length != 0) {
		 $(wrapper + " ul li a.active").parent().prev().find("a").click();
	  }
	  g.preventDefault(); 
  });
  
  $(wrapper + " " + container + " " + " a.next").click(function(h) {
	  if($(wrapper + " ul li a.active").parent().next().length != 0) {
		  $(wrapper + " ul li a.active").parent().next().find("a").click();
	  }
	  h.preventDefault(); 
  });
  
});