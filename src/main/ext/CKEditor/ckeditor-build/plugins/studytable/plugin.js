CKEDITOR.plugins.add('studytable', {
  init: function (editor) {
    var pluginName = 'studytable';
    editor.addCommand(pluginName, {

      exec: function (editor) {
      var htmlBody = (<r><![CDATA[     
	<table class="vrtx-courseplan-table">
	<tbody>
		<tr>
			<td class="small">
				6. semester</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
		</tr>
		<tr>
			<td class="small">
				5. semester</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
		</tr>
		<tr>
			<td class="small">
				4. semester</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
		</tr>
		<tr>
			<td class="small">
				3. semester</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
		</tr>
		<tr>
			<td class="small">
				2. semester</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
		</tr>
		<tr>
			<td class="small">
				1. semester</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
			<td class="large">
				&nbsp;</td>
		</tr>
		<tr>
			<td class="small">
				&nbsp;</td>
			<td class="large">
				10 studiepoeng</td>
			<td class="large">
				10 studiepoeng</td>
			<td class="large">
				10 studiepoeng</td>
		</tr>
	</tbody>
</table>
       ]]></r>).toString();  

     	       editor.insertHtml(htmlBody);  
      },
      canUndo: true
    });
    editor.ui.addButton('Studytable', {
      label: 'Insert study table',
      command: pluginName,
      icon: this.path + 'images/studytable.png'
    });
  }
});
