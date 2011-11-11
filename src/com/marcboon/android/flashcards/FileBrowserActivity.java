package com.marcboon.android.flashcards;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.eekboom.utils.Strings;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class FileBrowserActivity extends ListActivity implements OnItemLongClickListener {
	public final static String KEY_ROOT = "root";
	public final static String KEY_DATA = "data";
	public final static String DEFAULT_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
	public final static String SEPARATOR = System.getProperty("file.separator");
	public final static int SELECT_FILES = 1;
	
	private String root;
	private String folder;
	private Bundle selected;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set multiple selection mode
		ListView listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		
		// Set layout for empty list
		View empty = getLayoutInflater().inflate(R.layout.empty, null, false);
		listView.setEmptyView(empty);
		
		// Add long-click listener
		listView.setOnItemLongClickListener(this);

		// Get list of previously selected files from caller
		selected = getIntent().getBundleExtra(KEY_DATA);
		
		// Get root directory from caller, or use default
		root = getIntent().getStringExtra(KEY_ROOT);
		if(root == null) {
			root = DEFAULT_ROOT;
		}
		
		// Try to open root directory
		File file = new File(folder = root);
		if(file != null && file.canRead() && file.isDirectory()) {
			loadList(file);
			return;
		}

		// Can't read root directory, try default
		if(!root.equalsIgnoreCase(DEFAULT_ROOT)) {
			file = new File(folder = root = DEFAULT_ROOT);
			if(file != null && file.canRead() && file.isDirectory()) {
				loadList(file);
				return;
			}
		}

		// Can't read root folder, give up
		Toast.makeText(this, "Can't open " + root, Toast.LENGTH_LONG).show();
		finish();
	}

	/** Called when the menu button is pressed for the first time. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Create menu from resource file
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.browse, menu);
		return true;
	}

	/** Called when the user selects a menu item. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.done:
	    	done();
	    	break;
	    case R.id.cancel:
	    	cancel();
	    	break;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
    	return true;
	}

	/** Select file or open directory on item click */
	@Override
	public void onListItemClick(ListView l, View v, int pos, long id) {
		File file = new File(folder + SEPARATOR + l.getItemAtPosition(pos));
		if (file.canRead()) {
			if(file.isFile()) {
				setSelected(file, l.isItemChecked(pos));
			}
			else if(file.isDirectory()) {
				folder = file.getAbsolutePath();
				loadList(file);
			}
		}
	}

	/** Select directory on long item click */
	public boolean onItemLongClick(AdapterView<?> a, View v, int pos, long id) {
		File file = new File(folder + SEPARATOR + getListView().getItemAtPosition(pos));
		if (file.canRead() && file.isDirectory()) {
			setSelected(file, getListView().isItemChecked(pos));
		}
		return true;
	}

	private void setSelected(File file, boolean select) {
		String path = file.getAbsolutePath();
		Toast.makeText(this, path, Toast.LENGTH_SHORT);
		if(select) {
			selected.putBoolean(path, file.isDirectory());
		}
		else {
			selected.remove(path);
		}
	}
	
	@Override
	public void onBackPressed() {
		// Don't go above root folder
		if (folder.equalsIgnoreCase(root)) {
			return;
		}

		// Else navigate up the folder tree
		File parent = new File(folder).getParentFile();
		folder = parent.getAbsolutePath();
		loadList(parent);
	}
	
	protected void done() {
		// Return with list of selected files
		setResult(RESULT_OK, getIntent().putExtra(KEY_DATA, selected));
		finish();
	}
	
	protected void cancel() {
		// Return with result code CANCELED
		setResult(RESULT_CANCELED, null);
		finish();
	}
	
	protected void loadList(File dir) {
		// Get directory contents in natural sort order
		List<String> list = Arrays.asList(dir.list());
		Collections.sort(list, Strings.getNaturalComparatorIgnoreCaseAscii());
		
		// Load list with directory contents
		setTitle(folder.substring(root.lastIndexOf(SEPARATOR) + 1));
		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, list));
		
		// Set checkboxes according to selected state
		ListView listView = getListView();
		// If the folder is selected, all items in it are automatically selected
		if(selected.containsKey(folder)) {
			for(int i = 0; i < listView.getCount(); i++) {
				listView.setItemChecked(i, true);
			}
		}
		else {
			// Set selected state for each item individually
			for(int i = 0; i < listView.getCount(); i++) {
				String path = folder + SEPARATOR + listView.getItemAtPosition(i);
				if(selected.containsKey(path)) {
					listView.setItemChecked(i, true);
				}
			}
		}
	}

}