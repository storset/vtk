CKEDITOR.plugins.add('studytable', {
  init: function (editor) {
    var pluginName = 'studytable';
    editor.addCommand(pluginName, {

      exec: function (editor) {
      var htmlBody = "\
  <table class='vrtx-courseplan-table'>\n\
	<tbody>\n\
		<tr>\n\
			<td class='small'>\n\
				6. semester</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
		</tr>\n\
		<tr>\n\
			<td class='small'>\n\
				5. semester</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
		</tr>\n\
		<tr>\n\
			<td class='small'>\n\
				4. semester</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
		</tr>\n\
		<tr>\n\
			<td class='small'>\n\
				3. semester</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
		</tr>\n\
		<tr>\n\
			<td class='small'>\n\
				2. semester</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
		</tr>\n\
		<tr>\n\
			<td class='small'>\n\
				1. semester</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				&nbsp;</td>\n\
		</tr>\n\
		<tr>\n\
			<td class='small'>\n\
				&nbsp;</td>\n\
			<td class='large'>\n\
				10 studiepoeng</td>\n\
			<td class='large'>\n\
				10 studiepoeng</td>\n\
			<td class='large'>\n\
				10 studiepoeng</td>\n\
		</tr>\n\
	</tbody>\n\
</table>";

       	       var lang = editor.config.language;
	       if(lang == "en") 
		  htmlBody = htmlBody.replace(/studiepoeng/g, "Credits");       
     	       editor.insertHtml(htmlBody);  
      },
      canUndo: true
    });

    var lang = editor.config.language;
    if(lang == "en") 
	var pluginLabel = "Insert studytable";
    else
	var pluginLabel = "Sett inn studieløpstabell";

    editor.ui.addButton('Studytable', {
      label: pluginLabel,
      command: pluginName,
      icon: this.path + 'images/studytable.png'
    });
  }
});
