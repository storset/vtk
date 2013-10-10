/*
 *  VrtxDatepicker - facade to jQuery UI datepicker
 *  
 *  API: http://api.jqueryui.com/datepicker/
 *  
 *  * Requires Dejavu OOP library
 *  * Requires but Lazy-loads jQuery UI library (if not defined) on open
 */

var VrtxDatepickerInterface = dejavu.Interface.declare({
  $name: "VrtxDatepickerInterface"
});

var VrtxDatepicker = dejavu.Class.declare({
  $name: "VrtxDatepicker",
  $implements: [VrtxDatepickerInterface],
  __opts: {},
  initialize: function (opts) { // language, selector
    this.__opts = opts;
    var datepick = this;
    
    if(typeof opts.selector !== "undefined") {
      var contents = $(opts.selector);
    } else {
      var contents = $("#contents");
    }
    
    // i18n (default english)
    if (opts.language == 'no') {
      $.datepicker.setDefaults($.datepicker.regional['no']);
    } else if (opts.language == 'nn') {
      $.datepicker.setDefaults($.datepicker.regional['nn']);
    }
    
    datepick.initFields(contents.find(".date"));
    
    // Help user with time
    contents.on("change", ".vrtx-hours input", function () {
      var hh = $(this);
      var mm = hh.parent().nextAll(".vrtx-minutes").filter(":first").find("input"); // Relative to
      datepick.timeHelp(hh, mm);
    });
    contents.on("change", ".vrtx-minutes input", function () {
      var mm = $(this);
      var hh = mm.parent().prevAll(".vrtx-hours").filter(":first").find("input"); // Relative to
      datepick.timeHelp(hh, mm);
    });
    
    // Specific for start and end date
    var startDateElm = contents.find("#start-date-date");
    var endDateElm = contents.find("#end-date-date");
    if (startDateElm.length && endDateElm.length) {
      if (startDateElm.datepicker('getDate') != null) {
        datepick.setDefaultEndDate(startDateElm, endDateElm);
      }
      contents.on("change", "#start-date-date, #end-date-date", function () {
        datepick.setDefaultEndDate(startDateElm, endDateElm);
      }); 
    }
    
    if(opts.after) opts.after();
  },
  initFields: function(dateFields) {
    for(var i = 0, len = dateFields.length; i < len; i++) {
      this.initField(dateFields[i].name, this.__opts.selector);
    }
  },
  initField: function(name, selector) {
    var hours = "";
    var minutes = "";
    var date = [];
    var fieldName = name.replace(/\./g, '\\.');

    if(typeof selector !== "undefined") {
      var elem = $(selector + " #" + fieldName);
    } else {
      var elem = $("#" + fieldName);
    }
  
    if (elem.length) {
      hours = this.extractHoursFromDate(elem[0].value);
      minutes = this.extractMinutesFromDate(elem[0].value)
      date = new String(elem[0].value).split(" ");
    }

    var dateField = "<div class='vrtx-textfield vrtx-date'><input type='text' maxlength='10' size='8' id='" + name + "-date' value='" + date[0] + "' /></div>";
    var hoursField = "<div class='vrtx-textfield vrtx-hours'><input type='text' maxlength='2' size='1' id='" + name + "-hours' value='" + hours + "' /></div>";
    var minutesField = "<div class='vrtx-textfield vrtx-minutes'><input type='text' maxlength='2' size='1' id='" + name + "-minutes' value='" + minutes + "' /></div>";
    elem.parent().hide();
    elem.parent().after(dateField + hoursField + "<span class='vrtx-time-seperator'>:</span>" + minutesField);
    $("#" + fieldName + "-date").datepicker({
      dateFormat: 'yy-mm-dd',
      /* fix buggy IE focus functionality: 
       * http://www.objectpartners.com/2012/06/18/jquery-ui-datepicker-ie-focus-fix/ */
      fixFocusIE: false,
      /* blur needed to correctly handle placeholder text */
      onSelect: function(dateText, inst) {
        this.fixFocusIE = true;
        $(this).blur().change().focus();
      },
      onClose: function(dateText, inst) {
        this.fixFocusIE = true;
        this.focus();
      },
      beforeShow: function(input, inst) {
        var result = $.browser.msie ? !this.fixFocusIE : true;
        this.fixFocusIE = false;
        return result;
      }
    });
  },
  setDefaultEndDate: function(startDateElm, endDateElm) {
    var endDate = endDateElm.val();
    var startDate = startDateElm.datepicker('getDate');
    if (endDate == "") {
      endDateElm.datepicker('option', 'defaultDate', startDate);
    }
  },
  timeHelp: function(hh, mm) {
    var hhVal = hh.val();
    var mmVal = mm.val();
    if(hhVal.length || mmVal.length) {
      var newHhVal = this.timeRangeHelp(hhVal, 23);
      var newMmVal = this.timeRangeHelp(mmVal, 59);
      if((newHhVal == "00" || newHhVal == "0") && (newMmVal == "00" || newMmVal == "0")) { // If all zeroes => remove time
        hh.val("");
        mm.val("");
      } else {
        if(hhVal != newHhVal) hh.val(newHhVal);
        if(mmVal != newMmVal) mm.val(newMmVal);
      }
    }
  },
  timeRangeHelp: function(val, max) { /* XXX: better name */
    var newVal = parseInt(val, 10);
    if(isNaN(newVal) || newVal < 0) {
      newVal = "00";
    } else {
      newVal = (newVal > max) ? "00" : newVal;
      newVal = ((newVal < 10 && !newVal.length) ? "0" : "") + newVal;
    }
    return newVal;
  },
  extractHoursFromDate: function(datetime) {
    var a = new String(datetime);
    var b = a.split(" ");
    if (b.length > 1) {
      var c = b[1].split(":");
      if (c != null) {
        return c[0];
      }
    }
    return "";
  },
  extractMinutesFromDate: function(datetime) {
    var a = new String(datetime);
    var b = a.split(" ");
    if (b.length > 1) {
      var c = b[1].split(":");
      if (c.length > 0) {
        var min = c[1];
        if (min != null) {
          return min;
        }
        // Hour has been specified, but no minutes.
        // Return "00" to properly display time.
        return "00";
      }
    }
    return "";
  },
  prepareForSave: function() {
    var dateFields = $(".date");
    for(var i = 0, len = dateFields.length; i < len; i++) {
      var dateFieldName = dateFields[i].name;
      if (!dateFieldName) return;

      var fieldName = dateFieldName.replace(/\./g, '\\.');

      var hours = $("#" + fieldName + "-hours")[0];
      var minutes = $("#" + fieldName + "-minutes")[0];
      var date = $("#" + fieldName + "-date")[0];

      var savedVal = "";
    
      if (date && date.value.toString().length) {
        savedVal = date.value;
        if (hours && hours.value.toString().length) {
          savedVal += " " + hours.value;
          if (minutes.value && minutes.value.toString().length) {
           savedVal += ":" + minutes.value;
          }
        }
      }
      dateFields[i].value = savedVal;
    }
  }
});