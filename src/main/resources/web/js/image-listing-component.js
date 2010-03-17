// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, centered thumbnail navigation and fade effect
// by Øyvind Hatland - UiO / USIT

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, options) {
	  
	  //cache
	  var images = new Array();
	  
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
	  
	  initFirstImage();
		  
	  return this.each(function (i) {
		  
		   //center thumbnails images
		   var imgWidth = $("img", this).width();
		   if(imgWidth > $(this).width()) {
		      var leftAdjust = -(imgWidth - $(this).width()) / 2;
			  $("img", this).css("marginLeft", leftAdjust + "px");
		   }
		   
		   //generate image
		   var img = new Image();
		   var src = $("img", this).attr("src").split("?")[0]; img.src = src; img.alt = src;
			  
	       //generate link
	       link = document.createElement("a"); 
	       link.setAttribute("href", $(this).attr("href"));
	       link.setAttribute("class", container.substring(1) + "-link");
	       // IE
	       link.setAttribute("className", container.substring(1) + "-link");
	      
	       //append img inside link
	       $(link).append(img);
	      
	       //cache
	       images[i] = link;
		   
		   $(this).click(function(e) {

		  	  //replace link and image (w/ fade effect down to fadedOutOpacity) + stop() current animation.
		      if(settings.fadeInOutTime > 0) {
			      $(wrapper + " " + container).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
			    	  //done fade out -> remove
			    	  $("a" + container + "-link", this).remove();
			    	  //start fading in ...
			    	  $(this).fadeTo(settings.fadeInOutTime, 1);
			    	  //... before adding new image for smoother change
			    	  $(this).append(images[i]);  
			      });
		      } else {
		    	  $("a" + container + "-link", wrapper + " " + container).remove();
		    	  $(wrapper + " " + container).append(images[i]);   
		      }
		      
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
	  
	//TODO: refactor with above code
	function initFirstImage() {
	  //choose first image in <li>
	  $(wrapper + " ul li:first a").addClass("active");
		
	  //change image
	  var img = new Image();
	  var src = $(wrapper + " ul li:first a img").attr("src").split("?")[0]; img.src = src; img.alt = src;
	  
      //change link
      link = document.createElement("a"); 
      link.setAttribute("href", $(wrapper + " ul li:first a").attr("href"));
      link.setAttribute("class", container.substring(1) + "-link");
      // IE
      link.setAttribute("className", container.substring(1) + "-link");
      
      $("a" + container + "-link", wrapper + " " + container).remove();
      $(wrapper + " " + container).prepend(link);
	  $(link, wrapper + " " + container).append(img);
	}
	
  };
})(jQuery);