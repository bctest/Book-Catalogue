/*
 * @copyright 2011 Philip Warner
 * @license GNU General Public License
 * 
 * This file is part of Book Catalogue.
 *
 * Book Catalogue is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Book Catalogue is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Book Catalogue.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.eleybourn.bookcatalogue;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class Utils {
	private static String filePath = Environment.getExternalStorageDirectory() + "/" + BookCatalogue.LOCATION;
	private static String UTF8 = "utf8";
	private static int BUFFER_SIZE = 8192;

	private static ArrayUtils<Author> mAuthorUtils = null;
	private static ArrayUtils<Series> mSeriesUtils = null;

	static public ArrayUtils<Author> getAuthorUtils() {
		if (mAuthorUtils == null) {
			mAuthorUtils = new ArrayUtils<Author>(new Utils.Factory(){
				@Override
				public Object get(String source) {
					return new Author(source);
				}});			
		}
		return mAuthorUtils;
	}

	static public ArrayUtils<Series> getSeriesUtils() {
		if (mSeriesUtils == null) {
			mSeriesUtils = new ArrayUtils<Series>(new Utils.Factory(){
				@Override
				public Object get(String source) {
					return new Series(source);
				}});
		}
		return mSeriesUtils;
	}

	/**
	 * Check if the sdcard is writable
	 * 
	 * @return	success or failure
	 */
	static public boolean sdCardWritable() {
		/* Test write to the SDCard */
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath + "/.nomedia"), UTF8), BUFFER_SIZE);
			out.write("");
			out.close();
			return true;
		} catch (IOException e) {
			return false;
		}		
	}

	/**
	 * Encode a string by 'escaping' all instances of: '|', '\', \r, \n. The
	 * escape char is '\'.
	 * 
	 * This is used to build text lists separated by the passed delimiter.
	 * 
	 * @param s			String to convert
	 * @param delim		The list delimiter to encode (if found).
	 * 
	 * @return		Converted string
	 */
	static String encodeListItem(String s, char delim) {
		StringBuilder ns = new StringBuilder();
		for (int i = 0; i < s.length(); i++){
		    char c = s.charAt(i);        
		    switch (c) {
		    case '\\':
		    	ns.append("\\\\");
		    	break;
		    case '\r':
		    	ns.append("\\r");
		    	break;
		    case '\n':
		    	ns.append("\\n");
		    	break;
		    default:
		    	if (c == delim)
		    		ns.append("\\");
		    	ns.append(c);
		    }
		}
		return ns.toString();
	}

	/**
	 * Encode a list of strings by 'escaping' all instances of: delim, '\', \r, \n. The
	 * escape char is '\'.
	 * 
	 * This is used to build text lists separated by 'delim'.
	 * 
	 * @param s		String to convert
	 * @return		Converted string
	 */
	static String encodeList(ArrayList<String> sa, char delim) {
		StringBuilder ns = new StringBuilder();
		Iterator<String> si = sa.iterator();
		if (si.hasNext()) {
			ns.append(encodeListItem(si.next(), delim));
			while (si.hasNext()) {
				ns.append(delim);
				ns.append(encodeListItem(si.next(), delim));
			}
		}
		return ns.toString();
	}

	public interface Factory {
		Object get(String source);
	}

	static public class ArrayUtils<T> {

		Factory mFactory;

		ArrayUtils(Factory factory) {
			mFactory = factory;
		}

		@SuppressWarnings("unchecked")
		private T get(String source) {
			return (T) mFactory.get(source);
		}
		/**
		 * Encode a list of strings by 'escaping' all instances of: delim, '\', \r, \n. The
		 * escape char is '\'.
		 * 
		 * This is used to build text lists separated by 'delim'.
		 * 
		 * @param s		String to convert
		 * @return		Converted string
		 */
		String encodeList(ArrayList<T> sa, char delim) {
			Iterator<T> si = sa.iterator();
			return encodeList(si, delim);
		}

		private String encodeList(Iterator<T> si, char delim) {
			StringBuilder ns = new StringBuilder();
			if (si.hasNext()) {
				ns.append(encodeListItem(si.next().toString(), delim));
				while (si.hasNext()) {
					ns.append(delim);
					ns.append(encodeListItem(si.next().toString(), delim));
				}
			}
			return ns.toString();
		}
		
		/**
		 * Decode a text list separated by '|' and encoded by encodeListItem.
		 * 
		 * @param s		String representing the list
		 * @return		Array of strings resulting from list
		 */
		ArrayList<T> decodeList(String s, char delim) {
			StringBuilder ns = new StringBuilder();
			ArrayList<T> list = new ArrayList<T>();
			boolean inEsc = false;
			for (int i = 0; i < s.length(); i++){
			    char c = s.charAt(i);
			    if (inEsc) {
			    	switch(c) {
				    case '\\':
			    		ns.append(c);
				    	break;		    	
				    case 'r':
			    		ns.append('\r');
				    	break;		    	
				    case 't':
			    		ns.append('\t');
				    	break;		    	
				    case 'n':
			    		ns.append('\n');
				    	break;		    	
				    default:
				    	ns.append(c);
				    	break;
			    	}
		    		inEsc = false;
			    } else {
				    switch (c) {
				    case '\\':
			    		inEsc = true;
				    	break;
				    default:
				    	if (c == delim) {
					    	list.add(get(ns.toString()));
					    	ns.setLength(0);
					    	break;
				    	} else {
					    	ns.append(c);
					    	break;
				    	}
				    }
			    }
			}
			// It's important to send back even an empty item.
	    	list.add(get(ns.toString()));
			return list;
		}
	}
	/**
	 * Decode a text list separated by '|' and encoded by encodeListItem.
	 * 
	 * @param s		String representing the list
	 * @return		Array of strings resulting from list
	 */
	static ArrayList<String> decodeList(String s, char delim) {
		StringBuilder ns = new StringBuilder();
		ArrayList<String> list = new java.util.ArrayList<String>();
		boolean inEsc = false;
		for (int i = 0; i < s.length(); i++){
		    char c = s.charAt(i);
		    if (inEsc) {
		    	switch(c) {
			    case '\\':
		    		ns.append(c);
			    	break;		    	
			    case 'r':
		    		ns.append('\r');
			    	break;		    	
			    case 't':
		    		ns.append('\t');
			    	break;		    	
			    case 'n':
		    		ns.append('\n');
			    	break;		    	
			    default:
			    	ns.append(c);
			    	break;
		    	}
	    		inEsc = false;
		    } else {
			    switch (c) {
			    case '\\':
		    		inEsc = true;
			    	break;
			    default:
			    	if (c == delim) {
				    	list.add(ns.toString());
				    	ns.setLength(0);
				    	break;
			    	} else {
				    	ns.append(c);
				    	break;
			    	}
			    }
		    }
		}
		// It's important to send back even an empty item.
    	list.add(ns.toString());
		return list;
	}

	/**
	 * Add the current text data to the collection if not present, otherwise 
	 * append the data as a list.
	 * 
	 * @param key	Key for data to add
	 */
	static public void appendOrAdd(Bundle values, String key, String value) {
		String s = Utils.encodeListItem(value, '|');
		if (!values.containsKey(key) || values.getString(key).length() == 0) {
			values.putString(key, s);
		} else {
			String curr = values.getString(key);
			values.putString(key, curr + "|" + s);
		}
	}

	/**
	 * Given a URL, get an image and save to a file, optionally appending a suffic to the file.
	 * 
	 * @param urlText			Image file URL
	 * @param filenameSuffix	Suffix to add
	 *
	 * @return	Downloaded filespec
	 */
	static public String saveThumbnailFromUrl(String urlText, String filenameSuffix) {
		URL u;
		try {
			u = new URL(urlText);
		} catch (MalformedURLException e) {
			//Log.e("Book Catalogue", "Malformed URL");
			return "";
		}
		HttpURLConnection c;
		InputStream in = null;
		try {
			c = (HttpURLConnection) u.openConnection();
			c.setConnectTimeout(30000);
			c.setRequestMethod("GET");
			c.setDoOutput(true);
			c.connect();
			in = c.getInputStream();
		} catch (IOException e) {
			//Log.e("Book Catalogue", "Thumbnail cannot be read");
			return "";
		}

		String filename = "";
		FileOutputStream f = null;
		try {
			filename = CatalogueDBAdapter.fetchThumbnailFilename(0, true, filenameSuffix);
			f = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			//Log.e("Book Catalogue", "Thumbnail cannot be written");
			return "";
		}
		
		try {
			byte[] buffer = new byte[1024];
			int len1 = 0;
			while ( (len1 = in.read(buffer)) > 0 ) {
				f.write(buffer,0, len1);
			}
			f.close();
		} catch (IOException e) {
			//Log.e("Book Catalogue", "Error writing thumbnail");
			return "";
		}
		return filename;
	}

	/**
	 * Utility routine to get the data from a URL. Makes sure timeout is set to avoid application
	 * stalling.
	 * 
	 * @param url		URL to retrieve
	 * @return
	 * @throws UnknownHostException 
	 */
	static public InputStream getInputStream(URL url) throws UnknownHostException {
		int retries = 3;
		while (true) {
			try {
				java.net.URLConnection conn = url.openConnection();
				conn.setConnectTimeout(30000);
				return conn.getInputStream();
			} catch (java.net.UnknownHostException e) {
				Log.e("BookCatalogue.Utils", "Unknown Host in getInpuStream", e);
				retries--;
				if (retries-- == 0)
					throw e;
				try { Thread.sleep(500); } catch(Exception junk) {};
			} catch (Exception e) {
				/**
				 * TODO Handle some transient errors....like UnknownHostException...
				 */
				Log.e("BookCatalogue.Utils", "Exception in getInpuStream", e);
				throw new RuntimeException(e);
			}			
		}
	}

	/**
	 * If there is a '__thumbnails' key, pick the largest image, rename it
	 * and delete the others. Finally, remove the key.
	 * 
	 * @param result	Book data
	 */
	static public void cleanupThumbnails(Bundle result) {
    	if (result.containsKey("__thumbnail")) {
    		long best = -1;
    		int bestFile = -1;

    		// Parse the list
    		ArrayList<String> files = Utils.decodeList(result.getString("__thumbnail"), '|');

    		// Just read the image files to get file size
    		BitmapFactory.Options opt = new BitmapFactory.Options();
    		opt.inJustDecodeBounds = true;

    		// Scan, finding biggest
    		for(int i = 0; i < files.size(); i++) {
    			String filespec = files.get(i);
	    		File file = new File(filespec);
	    		if (file.exists()) {
		    	    BitmapFactory.decodeFile( filespec, opt );
		    	    // If no size info, assume file bad and skip
		    	    if ( opt.outHeight > 0 && opt.outWidth > 0 ) {
		    	    	long size = opt.outHeight * opt.outWidth;
		    	    	if (size > best) {
		    	    		best = size;
		    	    		bestFile = i;
		    	    	}
		    	    }	    		
	    		}
    		}

    		// Delete all but the best one. Note there *may* be no best one,
    		// so all would be deleted. We do this first in case the list 
    		// contains a file with the same name as the target of our
    		// rename.
    		for(int i = 0; i < files.size(); i++) {
    			if (i != bestFile) {
		    		File file = new File(files.get(i));
		    		file.delete();
    			}
    		}
    		// Get the best file (if present) and rename it.
			if (bestFile >= 0) {
	    		File file = new File(files.get(bestFile));
	    		file.renameTo(CatalogueDBAdapter.fetchThumbnail(0));
			}
    		// Finally, cleanup the data
    		result.remove("__thumbnail");
    	}			
	}

	public static String properCase(String inputString) {
		StringBuilder ff = new StringBuilder(); 
		String outputString;
		int wordnum = 0;

		try {
			for(String f: inputString.split(" ")) {
				if(ff.length() > 0) { 
					ff.append(" "); 
				} 
				wordnum++;
				String word = f.toLowerCase();
	
				if (word.substring(0,1).matches("[\"\\(\\./\\\\,]")) {
					wordnum = 1;
					ff.append(word.substring(0,1));
					word = word.substring(1,word.length());
				}
	
				/* Do not convert 1st char to uppercase in the following situations */
				if (wordnum > 1 && word.matches("a|to|at|the|in|and|is|von|de|le")) {
					ff.append(word);
					continue;
				} 
				try {
					if (word.substring(0,2).equals("mc")) {
						ff.append(word.substring(0,1).toUpperCase());
						ff.append(word.substring(1,2));
						ff.append(word.substring(2,3).toUpperCase());
						ff.append(word.substring(3,word.length()));
						continue;
					}
				} catch (StringIndexOutOfBoundsException e) {
					// do nothing and continue;
				}
	
				try {
					if (word.substring(0,3).equals("mac")) {
						ff.append(word.substring(0,1).toUpperCase());
						ff.append(word.substring(1,3));
						ff.append(word.substring(3,4).toUpperCase());
						ff.append(word.substring(4,word.length()));
						continue;
					}
				} catch (StringIndexOutOfBoundsException e) {
					// do nothing and continue;
				}
	
				try {
					ff.append(word.substring(0,1).toUpperCase());
					ff.append(word.substring(1,word.length()));
				} catch (StringIndexOutOfBoundsException e) {
					ff.append(word);
				}
			}
	
			/* output */ 
			outputString = ff.toString();
		} catch (StringIndexOutOfBoundsException e) {
			//empty string - do nothing
			outputString = inputString;
		}
		return outputString;
	}

	/**
	 * Join the passed array of strings, with 'delim' between them.
	 * 
	 * @param sa		Array of strings to join
	 * @param delim		Delimiter to place between entries
	 * 
	 * @return			The joined strings
	 */
	static String join(String[] sa, String delim) {
		// Simple case, return empty string
		if (sa.length <= 0)
			return "";

		// Initialize with first
		StringBuilder buf = new StringBuilder(sa[0]);

		if (sa.length > 1) {
			// If more than one, loop appending delim then string.
			for(int i = 1; i < sa.length; i++) {
				buf.append(delim);
				buf.append(sa[i]);
			}
		}
		// Return result
		return buf.toString();
	}

	/**
	 * Method added mainly to avoid @SuppressWarnings on larger methods.
	 */
	public static ArrayList<Author> getAuthorsFromIntent(Intent i) {
		return i.getParcelableArrayListExtra(CatalogueDBAdapter.KEY_AUTHOR_ARRAY);
	}

	/**
	 * Method added mainly to avoid @SuppressWarnings on larger methods.
	 */
	public static ArrayList<Author> getAuthorsFromBundle(Bundle b) {
		return b.getParcelableArrayList(CatalogueDBAdapter.KEY_AUTHOR_ARRAY);
	}

	public static long getAsLong(Bundle b, String key) {
		Object o = b.get(key);
		if (o instanceof Long) {
			return (Long) o;
		} else if (o instanceof String) {
			return Long.parseLong((String)o);
		} else if (o instanceof Integer) {
			return ((Integer)o).longValue();
		} else {
			throw new RuntimeException("Not a long value");
		}
	}

	public static String getAsString(Bundle b, String key) {
		Object o = b.get(key);
		return o.toString();
	}

	// TODO Add author_aliases table to allow further pruning (eg. Joe Haldeman == Jow W Haldeman).
	public static void pruneAuthors(CatalogueDBAdapter db, ArrayList<Author> authors) {
		Hashtable<String,Boolean> names = new Hashtable<String,Boolean>();

		for(int i = authors.size() - 1; i >=0; i--) {
			Author a = authors.get(i);
			String name = a.getSortName();
			if (names.containsKey(name)) {
				authors.remove(i);
			} else {
				names.put(name, true);
				if (a.id == 0)
					a.id = db.lookupAuthorId(a);
			}
		}
	}
}

