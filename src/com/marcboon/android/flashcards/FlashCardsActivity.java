package com.marcboon.android.flashcards;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

public class FlashCardsActivity extends Activity {
	public final static String ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
	public final static String DATA = ROOT + "/marcboon.com/FlashCards";
	public final static String VIEW = ".view";
	public final static String TXT = ".txt";
	public final static String UTF = ".utf";
	public final static int HANZI = 0;
	public final static int PINYIN = 1;
	public final static int ENGLISH = 2;
	public final static String[] VIEWS = { "汉字", "pinyin", "English" };
	public final static Card hello = new Card("你好", "nǐ hǎo", "hello");

	private SharedPreferences prefs;
	private TextView text;
	private Vector<Card> stack = new Vector<Card>();
	private Random random;
	private int card;
	private int previousCard;
	private int view;
	private int firstView = HANZI;
	private Bundle decks;

	private GestureDetector gestureDetector;
	private GestureListener gestureListener;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Create UI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		text = (TextView) findViewById(R.id.text);

		// Register gesture listener
		gestureListener = new GestureListener();
		gestureDetector = new GestureDetector(this, gestureListener);

		// Load saved view and card stack
		prefs = getPreferences(0);
		firstView = prefs.getInt(VIEW, HANZI);
		decks = new Bundle();
		for(String deck : prefs.getAll().keySet()) {
			if(!deck.startsWith(".")) {
				decks.putBoolean(deck, true);
			}
		}
		loadStack();
	}

	/** Called when the activity is started. */
	@Override
	protected void onStart() {
		super.onStart();

		// Initialize random generator
		random = new Random();
	}

	/** Called when the activity is stopped. */
	@Override
	protected void onStop() {
		super.onStop();

		// Save view mode and selected decks
		SharedPreferences.Editor ed = prefs.edit();
		ed.clear();
		ed.putInt(".view", firstView);
		for (String deck : decks.keySet()) {
			ed.putBoolean(deck, true);
		}
		ed.commit();
	}

	/** Called when the menu button is pressed for the first time. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Create menu from resource file
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	/** Called every time the menu button is pressed. */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// Select current view mode
		MenuItem item;
		switch (view) {
		case HANZI:
			item = menu.findItem(R.id.view_hanzi);
			if (item != null) {
				item.setChecked(true);
			}
			break;
		case PINYIN:
			item = menu.findItem(R.id.view_pinyin);
			if (item != null) {
				item.setChecked(true);
			}
			break;
		case ENGLISH:
			item = menu.findItem(R.id.view_english);
			if (item != null) {
				item.setChecked(true);
			}
			break;
		}
		return true;
	}

	/** Called when the user selects a menu item. */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.view_hanzi:
			view = HANZI;
			break;
		case R.id.view_pinyin:
			view = PINYIN;
			break;
		case R.id.view_english:
			view = ENGLISH;
			break;
		case R.id.decks:
			Intent intent = new Intent(this, FileBrowserActivity.class);
			intent.putExtra(FileBrowserActivity.KEY_ROOT, DATA);
			intent.putExtra(FileBrowserActivity.KEY_DATA, decks);
			startActivityForResult(intent, FileBrowserActivity.SELECT_FILES);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
		setView(view);
		return true;
	}

	/** Called when the activity called with startActivityForResult() returns */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK && requestCode == FileBrowserActivity.SELECT_FILES) {
			// Load card stack
			decks = data.getBundleExtra(FileBrowserActivity.KEY_DATA);
			loadStack();
		}
	}

	/** Called on any key press, here used for 5-way navigation keys */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			previousView();
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			nextView();
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			previousCard();
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			nextCard();
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			randomCard();
			return true;
		case KeyEvent.KEYCODE_BACK:
			card = previousCard;
			view = firstView;
			showCard();
			return true;
		}
		return false;
	}

	/**
	 * Touch events are parsed with our gesture detector, implemented as an
	 * inner class
	 */
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return gestureDetector.onTouchEvent(e);
	}

	/** Cycle through previous view */
	public void previousView() {
		if (--view < 0) {
			view = VIEWS.length - 1;
		}
		showCard();
	}

	/** Cycle through next view */
	public void nextView() {
		if (++view >= VIEWS.length) {
			view = 0;
		}
		showCard();
	}

	/** Cycle through previous card */
	public void previousCard() {
		previousCard = card;
		if(stack.size() > 0 && --card < 0) {
			card = stack.size() - 1;
		}
		view = firstView;
		showCard();
	}
	
	/** Cycle through next card */
	public void nextCard() {
		previousCard = card;
		if(++card >= stack.size()) {
			card = 0;
		}
		view = firstView;
		showCard();
	}

	/** Pick a random card */
	public void randomCard() {
		if(stack.size() > 2) {
			previousCard = card;
			card += random.nextInt(stack.size() - 1) + 1;
			if(card >= stack.size()) {
				card -= stack.size();
			}
			view = firstView;
			showCard();
		}
		else nextCard();
	}

	/** Show current card */
	public void showCard() {
		if (text != null) {
			Card current = stack.elementAt(card);
			setTitle((card + 1) + "/" + stack.size() + " " + VIEWS[view]);
			switch(view) {
			case HANZI:
				text.setText(current.hanzi);
				break;
			case PINYIN:
				text.setText(current.pinyin);
				break;
			case ENGLISH:
				text.setText(current.english);
				break;
			}
		}
	}

	/** Set view mode */
	public void setView(int view) {
		this.view = view;
		firstView = view;
		showCard();
	}

	/** Empty card stack */
	public void clear() {
		decks.clear();
		stack.removeAllElements();
		previousCard = card = 0;
		view = firstView;
	}
	
	protected void loadStack() {
		// Empty card stack
		stack.clear();
		Bundle loaded = new Bundle();

		// Load selected decks
		for (String deck : decks.keySet()) try {
			if(loadFile(new File(deck))) {
				loaded.putBoolean(deck, decks.getBoolean(deck));
			}
		}
		catch (IOException e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}
		decks = loaded;
		Toast.makeText(this, "Decks: " + decks.size(), Toast.LENGTH_SHORT).show();

		// Load default card if stack is empty
		if(stack.size() == 0) {
			stack.add(hello);
		}
		previousCard = card = 0;
		view = firstView;
		showCard();
	}

	protected boolean loadFile(File file) throws IOException {
		if(file.canRead()) {
			if(file.isFile()) {
				// Open stream
				InputStream is = new FileInputStream(file);
				if (file.getName().endsWith(TXT)) {
					return loadFromInputStream(is);
				}
				else if (file.getName().endsWith(UTF)) {
					return loadUTF(new DataInputStream(is));
				}
			}
/*
			else if(file.isDirectory()) {
				// Recursively load all files in directory
				for (String fname : file.list()) {
					loadFile(new File(fname));
				}
			}
*/
		}
		else {
			Toast.makeText(this, "Can't read " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
		}
		return false;
	}

	protected boolean loadUTF(DataInputStream is) throws IOException {
		try {
			while (true) {
				String line = is.readUTF();
                // Add new card 
                stack.add(new Card(line));
			}
		} catch (EOFException e) {
		} finally {
			is.close();
		}
		return true;
	}

	protected boolean loadFromInputStream(InputStream is) throws IOException {
		Reader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IOException();
		}
		// Read cards line by line until EOF
		String line = null;
		while ((line = readLine(reader)) != null) {
            // Add new card 
            stack.add(new Card(line));
		}
		reader.close();
		return true;
	}

	protected String readLine(Reader reader) throws IOException {
		// Test whether the end of file has been reached. If so, return null.
		int readChar = reader.read();
		if (readChar == -1) {
			return null;
		}
		StringBuffer string = new StringBuffer();
		// Read until end of file or new line
		while (readChar != -1 && readChar != '\n') {
			// Append the read character to the string. Some operating systems
			// such as Microsoft Windows prepend newline character ('\n') with
			// carriage return ('\r'). This is part of the newline character
			// and therefore an exception that should not be appended to the
			// string.
			if (readChar != '\r') {
				string.append((char) readChar);
			}
			// Read the next character
			readChar = reader.read();
		}
		return string.toString();
	}

	/** Touch event gesture listener, implemented as an inner class */
	protected class GestureListener extends SimpleOnGestureListener {
		static final float SWIPE_MIN_DIST = 50f; // min. 50 px
		static final float SWIPE_MAX_AXIS = 0.5f; // max. 30 degrees off axis

		/** Single tap emulates the center key on the 5-way navigation button */
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			randomCard();
			return true;
		}

		/** Swipes emulate the 4 direction keys of the 5-way navigation button */
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float x, float y) {
			float dx = e2.getX() - e1.getX();
			float dy = e2.getY() - e1.getY();
			if (Math.abs(dx) > SWIPE_MIN_DIST
					&& Math.abs(dy / dx) < SWIPE_MAX_AXIS) {
				if (dx > 0) {
					// swipe right
					nextCard();
				} else {
					// swipe left
					previousCard();
				}
				return true;
			} else if (Math.abs(dy) > SWIPE_MIN_DIST
					&& Math.abs(dx / dy) < SWIPE_MAX_AXIS) {
				if (dy > 0) {
					// swipe down
					nextView();
				} else {
					// swipe up
					previousView();
				}
				return true;
			}
			// undefined swipe
			return false;
		}
	}
}
