CKEDITOR.plugins.add( 'studyreferencecomponent',
{
	init: function( editor )
	{
		var pluginName = 'studyreferencecomponent';
		editor.addCommand( pluginName, new CKEDITOR.dialogCommand( pluginName ) );
 
		editor.ui.addButton( 'studyreferencecomponent',
		{
			label: 'Sett inn referansekomponent for studieløp',
			command: pluginName,
			icon: this.path + 'images/icon.png'
		} );
 
		CKEDITOR.dialog.add( pluginName, function( editor )
		{
			return {
				title : 'Sett inn referansekomponent for studieløp',
				minWidth : 400,
				minHeight : 100,
				contents :
				[
					{
						id : 'general',
						label : 'Settings',
						elements :
						[
							{
								type : 'select',
								id : 'type',
								label : 'Type',
								items : 
								[
									[ 'emne'],
									[ 'emnegruppe'],
									[ 'semester'],
									[ 'studieprogram'],
									[ 'studieretning']
								],
								commit : function( data )
								{
									data.studietype = this.getValue();
								}
							},
							{
								type : 'text',
								id : 'value',
								label : 'Value',
								validate : CKEDITOR.dialog.validate.notEmpty( 'Du må skrive inn en verdi.' ),
								required : true,
								commit : function( data )
								{
									data.tekstverdi = this.getValue();
								}
							},
						]
					}
				],
				onOk : function()
				{
					var dialog = this,
						data = {},
						output = (<r><![CDATA[
						${resource:ref referencetype=[TYPE] value=[VALUE]}
						]]></r>).toString();  

					this.commitContent( data );
					output = output.replace("TYPE", data.studietype);
					output = output.replace("VALUE", data.tekstverdi);						

					editor.insertHtml( output );
				}
			};
		} );
	}
} );
