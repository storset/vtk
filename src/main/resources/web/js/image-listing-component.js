// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, fade effect
// by Øyvind Hatland - UiO / USIT

// ('load') so that all images is loaded before running
// and .bind for performance increase: http://jqueryfordesigners.com/demo/fade-method2.html
$(window).bind("load", function () {
	
  var wrapper = ".vrtx-image-listing-include";	
  var container = ".vrtx-image-listing-include-container";

  // Unobtrusive JavaScript
  $(container + "-pure-css").addClass("vrtx-image-listing-include-container");	
  
  alert(container);
  $(wrapper + " ul li a").vrtxSGallery();
	
  //choose first image in <li>
  $(wrapper + " ul li:first a").click();
	  
});

(function ($) {
  $.fn.vrtxSGallery = function (options) {
	  
  var wrapper = ".vrtx-image-listing-include";
  var container = ".vrtx-image-listing-include-container";
  var fadeInOutTime = 250; //ms
  var fadedOutOpacity = 0;
	  
  $(container + "-nav-pure-css").addClass("vrtx-image-listing-include-container-nav");
  
  //paging (relative to li a.active)
  $(wrapper + " " + " a.prev").click(function(g) {
	if($(wrapper + " ul li a.active").parent().prev().length != 0) {
	  $(wrapper + " ul li a.active").parent().prev().find("a").click();
	} else {
	  $(wrapper + " ul li:last a").click();   
	}
    g.preventDefault(); 
  });
	  
  $(wrapper + " " + " a.next").click(function(h) {
	if($(wrapper + " ul li a.active").parent().next().length != 0) {
      $(wrapper + " ul li a.active").parent().next().find("a").click();
	} else {
	  $(wrapper + " ul li:first a").click();  
	}
	h.preventDefault(); 
  });
	  
  return this.each(function (i) {
	       
	       $(this).click(function(e) {
	    	  
	    	  //change image
	    	  var img = new Image();
	    	  var src = $("img", this).attr("src").split("?")[0]; img.src = src; img.alt = src;
	    	  
	          //change link
	          link = document.createElement("a"); 
	          link.setAttribute("href", $(this).attr("href"));
	          link.setAttribute("class", "vrtx-image-listing-include-container-link");
	          // IE
	          link.setAttribute("className", "vrtx-image-listing-include-container-link");
	          
	      	  //replace link and image (w/ fade effect down to fadedOutOpacity) + stop() current animations.
	          $(wrapper + " " + container).stop().fadeTo(fadeInOutTime, fadedOutOpacity, function() {
	        	  //done fade out -> remove
	        	  $("a.vrtx-image-listing-include-container-link", this).remove();
	        	  //start fading in ...
	        	  $(this).fadeTo(fadeInOutTime, 1);
	        	  //... before adding new image for smoother change
	        	  $(this).prepend(link);
	        	  $(link, this).append(img);
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
  
  };
        
})(jQuery);