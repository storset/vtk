/*
 * View Toggle
 * 
 * - Store cached refs and i18n at init in configs-obj (on toggle link id)
 * 
 */

(function(){
  if(typeof Toggler !== "function") {
    function Toggler() {
      this.configs = {};
    }
    toggler = new Toggler();
    
    $(document).ready(function() {
      toggler.init();
    });
  
    Toggler.prototype.add = function(config) {
      this.configs["vrtx-" + config.name + "-toggle"] = config;
    };
  	  
    Toggler.prototype.init = function() {
	  var self = this;

      for(var key in self.configs) {
    	var config = self.configs[key];
        var container = $("#vrtx-" + config.name);
	    var link = $("#" + key);
	    if(container.length && link.length) {
	      container.hide();
	      link.addClass("togglable");
	      link.parent().show();
	      config.container = container;
	      config.link = link;
	    }
      }
      
      $(document).on("click", "a.togglable", function(e) {
	    self.toggle(this);
	    e.stopPropagation();
	    e.preventDefault();
      });
    };
  
    Toggler.prototype.toggle = function(link) {
	   var config = this.configs[link.id];
	   if(config.isAnimated) {
	     config.container.slideToggle("fast"); /* XXX: proper easing requires jQuery UI */ 
	   } else {
	     config.container.toggle();   
	   }
	   if(config.container.filter(":visible").length) {
	     config.link.text(config.hideLinkText);
	   } else {
	     config.link.text(config.showLinkText);
	   }
    };
  }
})();