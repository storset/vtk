CKEDITOR.plugins.add('studytable', {
  init: function (editor) {
    var pluginName = 'studytable';
    editor.addCommand(pluginName, {

      exec: function (editor) {
      var htmlBody = (<r><![CDATA[     
	<table class="vrtx-courseplan-table" style="width:100%">
	<tbody>
		<tr>
			<td style="white-space: nowrap; width: 10%;">
				6. semester</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
		</tr>
		<tr>
			<td style="white-space: nowrap; width: 10%;">
				5. semester</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
		</tr>
		<tr>
			<td style="white-space: nowrap; width: 10%;">
				4. semester</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
		</tr>
		<tr>
			<td style="white-space: nowrap; width: 10%;">
				3. semester</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
		</tr>
		<tr>
			<td style="white-space: nowrap; width: 10%;">
				2. semester</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
		</tr>
		<tr>
			<td style="white-space: nowrap; width: 10%;">
				1. semester</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
			<td style="width: 30%">
				&nbsp;</td>
		</tr>
		<tr>
			<td style="white-space: nowrap; width: 10%;">
				&nbsp;</td>
			<td style="width: 30%">
				10 studiepoeng</td>
			<td style="width: 30%">
				10 studiepoeng</td>
			<td style="width: 30%">
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
