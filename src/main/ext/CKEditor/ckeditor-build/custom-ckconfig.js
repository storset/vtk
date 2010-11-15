CKEDITOR.editorConfig = function( config )
{

config.forcePasteAsPlainText = true;
config.toolbarCanCollapse = false;
config.disableNativeSpellChecker = false;
config.browserContextMenuOnCtrl = true;

config.extraPlugins = 'MediaEmbed';

config.toolbar_Complete = [
            ['Source','PasteText','PasteFromWord','-','Undo','Redo','-','Replace','RemoveFormat','-','Link','Unlink','Anchor','Image','MediaEmbed','Table','HorizontalRule','SpecialChar'],
            ['Format','-','Bold','Italic','Underline','Strike','Subscript','Superscript','NumberedList','BulletedList','Outdent','Indent','JustifyLeft','JustifyCenter','JustifyRight','TextColor','Maximize']
] ;

config.toolbar_Complete_article = [
            ['Source','PasteText','PasteFromWord','-','Undo','Redo','-','Replace','RemoveFormat','-','Link','Unlink','Anchor','Image','CreateDiv','MediaEmbed','Table','HorizontalRule','SpecialChar'],
            ['Format','-','Bold','Italic','Underline','Strike','Subscript','Superscript','NumberedList','BulletedList','Outdent','Indent','JustifyLeft','JustifyCenter','JustifyRight','TextColor','Maximize']
] ;

config.toolbar_Inline = [
            ['Source','PasteText','PasteFromWord','Link','Unlink', 'Bold','Italic','Underline','Strike','Subscript','Superscript','SpecialChar']
] ;

config.toolbar_Inline_S = [
            ['Source','PasteText','PasteFromWord','Link','Unlink', 'Bold','Italic','Underline','Strike','SpecialChar']
] ;

config.toolbar_Vortikal = [
            ['Save','-','PasteText','PasteFromWord','-','Undo','Redo','-','Replace','RemoveFormat','-','Link','Unlink','Anchor','Image','MediaEmbed','Table','HorizontalRule','SpecialChar'],
 	    '/',
            ['Format','-','Bold','Italic','Underline','Strike','Subscript','Superscript','NumberedList','BulletedList','Outdent','Indent','JustifyLeft','JustifyCenter','JustifyRight','TextColor','Maximize'],
 	    '/'
] ;

config.toolbar_AddComment = [
            ['Source', 'Bold','Italic','Underline','Strike','NumberedList','BulletedList','Link','Unlink']
] ;
};
