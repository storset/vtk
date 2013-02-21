/*
 * View Toggle
 * 
 * - Store cached refs and i18n at init in configs-obj (on toggle link id)
 * 
 * XXX: JSDoc
 * 
 */
(function () {
  if (typeof Toggler !== "function") {
    function Toggler() {
      this.configs = {
        /* name 
         * hideLinkText
         * showLinkText
         * combinator (optional)
         * isAnimated (optional)
         */
      };
    }
    toggler = new Toggler(); /* Global accessible object - XXX: proper singleton */

    $(document).ready(function () {
      toggler.init();
    });

    Toggler.prototype.add = function (config) {
      this.configs["vrtx-" + config.name + "-toggle"] = config;
    };

    Toggler.prototype.init = function () {
      var self = this;

      for (var key in self.configs) {
        var config = self.configs[key];
        var container = null;
        if(config.combinator) {
          container = $(config.combinator + "." + config.name);
        } else {
          container = $("#vrtx-" + config.name);
        }
        var link = $("#" + key);
        if (container.length && link.length) {
          container.hide();
          link.addClass("togglable");
          link.parent().show();
          config.container = container;
          config.link = link;
        }
      }

      $(document).on("click", "a.togglable", function (e) {
        self.toggle(this);
        e.stopPropagation();
        e.preventDefault();
      });
    };

    Toggler.prototype.toggle = function (link) {
      var self = this;

      var config = self.configs[link.id];
      if (config.isAnimated) {
        config.container.slideToggle("fast", function () { /* XXX: proper easing requires jQuery UI */
          self.toggleLinkText(config);
        });
      } else {
        config.container.toggle();
        self.toggleLinkText(config);
      }
    };

    Toggler.prototype.toggleLinkText = function (config) {
      if (config.container.filter(":visible").length) {
        config.link.text(config.hideLinkText);
      } else {
        config.link.text(config.showLinkText);
      }
    };
  }
})();