//loading thickbox message with check for unsaved changes in editor
$(document).ready(function () {
  tb_init_check_props('a.thickbox-check-unsaved-changes, area.thickbox-check-unsaved-changes, input.thickbox-check-unsaved-changes'); //pass where to apply thickbox
});

function tb_init_check_props(domChunk) {
  $(domChunk).click(function () {
    var t = this.title || this.name || null;
    var a = this.href || this.alt;
    var g = this.rel || false;
    if (typeof (NEED_TO_CONFIRM) != "undefined") {
      tb_show_check_props(t, a, g);
    } else {
      tb_show(t, a, g);
    }
    this.blur();
    return false;
  });
}

function tb_show_check_props(caption, url, imageGroup) {
  var checkForChanges = unsavedChangesInEditor();
  if (checkForChanges != undefined) {
    if (!confirm(COMPLETE_UNSAVED_CHANGES_CONFIRMATION)) {
      return false;
    }
    NEED_TO_CONFIRM = false;
  }
  tb_show(caption, url, imageGroup);
}