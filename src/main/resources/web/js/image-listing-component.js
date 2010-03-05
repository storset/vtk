// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, fade effect
// by Øyvind Hatland - UiO / USIT

// ('load') so that all images is loaded before running
// and .bind for performance increase: http://jqueryfordesigners.com/demo/fade-method2.html
$(window).bind("load", function () {
 
  var wrapper = ".vrtx-image-listing-include";	
  var container = ".vrtx-image-listing-include-container";
  
  $(wrapper + " ul li a").vrtxSGallery(wrapper, container);
	
  //choose first image in <li>
  $(wrapper + " ul li:first a").click();
	  
});

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, options) {
	 
  //animation settings
  settings = jQuery.extend({
	fadeInOutTime : 250,
	fadedOutOpacity: 0
  }, options);
  
  //Unobtrusive JavaScript
  $(container + "-pure-css").addClass(container.substring(1));
  $(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
	  
  //paging (relative to li a.active)
  addPagingClickEvent("next", wrapper);
  addPagingClickEvent("prev", wrapper);  
  
  function addPagingClickEvent(navClass, wrapper) {
    $(wrapper + " " + " a." + navClass).click(function(h) {
      if(navClass == "next") {
		  if($(wrapper + " ul li a.active").parent().next().length != 0) {
		    $(wrapper + " ul li a.active").parent().next().find("a").click();
		  } else {
			$(wrapper + " ul li:first a").click();
		  }
      } else {
    	  if($(wrapper + " ul li a.active").parent().prev().length != 0) {
    		$(wrapper + " ul li a.active").parent().prev().find("a").click();
          } else {
    		$(wrapper + " ul li:last a").click();   
          }
      }
	  h.preventDefault(); 
	});  
  }
	  
  return this.each(function (i) {
	  
	   //center thumbnails images
	   var imgWidth = $("img", this).width();
	   if(imgWidth > $(this).width()) {
	      var leftAdjust = -(imgWidth - $(this).width()) / 2;
		  $("img", this).css("marginLeft", leftAdjust + "px");
	   }
	   
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
	      
	  	  //replace link and image (w/ fade effect down to fadedOutOpacity) + stop() current animation.
	      $(wrapper + " " + container).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
	    	  //done fade out -> remove
	    	  $("a.vrtx-image-listing-include-container-link", this).remove();
	    	  //start fading in ...
	    	  $(this).fadeTo(settings.fadeInOutTime, 1);
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