CKEDITOR.plugins.add('ajaxsave', {
  init: function (editor) {
    var pluginName = 'ajaxsave';
    editor.addCommand(pluginName, {
      exec: function (editor) {
        var $form = editor.element.$.form;
        if ($form) {
          try {
            for (instance in CKEDITOR.instances) {
              CKEDITOR.instances[instance].updateElement();
            }
           
            tb_show(saveDocAjaxText + "...", 
                   "/vrtx/__vrtx/static-resources/js/plugins/thickbox-modified/loadingAnimation.gif?width=240&height=20", 
                   false);
            
            $("#editor").ajaxSubmit({
              success: function () {
                performSave();
              },
              complete: function() {
                tb_remove();
              }
            });
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
