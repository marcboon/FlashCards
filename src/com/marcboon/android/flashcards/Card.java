package com.marcboon.android.flashcards;

import java.io.UnsupportedEncodingException;

import android.content.ContentValues; 


public class Card {
	public final static String HANZI = "hanzi";
	public final static String PINYIN = "pinyin";
	public final static String ENGLISH = "english";

	public String hanzi;
	public String pinyin;
	public String english;
	
	public Card(String hanzi, String pinyin, String english) {
		this.hanzi = hanzi;
		this.pinyin = pinyin;
		this.english = english;
	}
	
	public Card(String packed) {
		try {
			int i = packed.indexOf(0x09);
			if(i > 0) {
				hanzi = packed.substring(0, i++);
				int j = packed.indexOf(0x09, i);
				if(j > 0) {
					pinyin = packed.substring(i, j++);
					english = packed.substring(j).trim();
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String toString() {
		return hanzi + "\0x09" + pinyin + "\0x09" + english;
	}
	
	public ContentValues getValues() {
		ContentValues values = new ContentValues();
		
		values.put(HANZI, hanzi);
		values.put(PINYIN, pinyin);
		values.put(ENGLISH, english);
		
		return values;
	}

	public byte[] getBytes() {
		byte[] bytes = null;
		try {
			bytes = toString().getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return bytes;
	}
}
