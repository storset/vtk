CKEDITOR.plugins.add('ajaxsave', {
  init: function (editor) {
    var pluginName = 'ajaxsave';
    editor.addCommand(pluginName, {
      exec: function (editor) {
        var $form = editor.element.$.form;
        if ($form) {
          try {
          	if($form.workingCopy) {       
            }
            else {
            	documentSave();
            }
          }
          catch (e) {
            alert(e);
          }
        }
      },
      canUndo: true
    });
    editor.ui.addButton('Ajaxsave', {
      label: 'Save Ajax',
      command: pluginName,
      className: 'cke_button_save'
    });
  }
});
