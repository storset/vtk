update vortex_resource 
	set CHARACTER_ENCODING = 'utf-8'  
	where content_type='application/json';
	
update vortex_resource 
	set GUESSED_CHARACTER_ENCODING = 'utf-8'  
	where content_type='application/json';

commit;