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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import com.eleybourn.bookcatalogue.goodreads.GoodreadsRegister;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StandardDialogs {

	public static void needLibraryThingAlert(final Context context, final boolean ltRequired, final String prefSuffix) {
		boolean showAlert;
		int msgId;
		final String prefName = LibraryThingManager.LT_HIDE_ALERT_PREF_NAME + "_" + prefSuffix;
		if (!ltRequired) {
			msgId = R.string.uses_library_thing_info;
			SharedPreferences prefs = context.getSharedPreferences("bookCatalogue", android.content.Context.MODE_PRIVATE);
			showAlert = !prefs.getBoolean(prefName, false);
		} else {
			msgId = R.string.require_library_thing_info;
			showAlert = true;
		}

		if (!showAlert)
			return;

		final AlertDialog dlg = new AlertDialog.Builder(context).setMessage(msgId).create();
		
		dlg.setTitle(R.string.reg_library_thing_title);
		dlg.setIcon(android.R.drawable.ic_menu_info_details);

		dlg.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.more_info), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent i = new Intent(context, AdministrationLibraryThing.class);
				context.startActivity(i);
				dlg.dismiss();
			}
		});

		if (!ltRequired) {
			dlg.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.disable_dialogue), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					SharedPreferences prefs = context.getSharedPreferences("bookCatalogue", android.content.Context.MODE_PRIVATE);
					SharedPreferences.Editor ed = prefs.edit();
					ed.putBoolean(prefName, true);
					ed.commit();
					dlg.dismiss();
				}
			});			
		}

		dlg.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dlg.dismiss();
			}
		}); 

		dlg.show();
	}

	public static void deleteSeriesAlert(Context context, final CatalogueDBAdapter dbHelper, final Series series, final Runnable onDeleted) {

		// When we get here, we know the names are genuinely different and the old series is used in more than one place.
		String message = "Delete series";
		try {
			message = String.format(context.getResources().getString(R.string.really_delete_series), series.name);
		} catch (NullPointerException e) {
			Logger.logError(e);
		}
		final AlertDialog alertDialog = new AlertDialog.Builder(context).setMessage(message).create();

		alertDialog.setTitle(R.string.delete_series);
		alertDialog.setIcon(android.R.drawable.ic_menu_info_details);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dbHelper.deleteSeries(series);
				alertDialog.dismiss();
				onDeleted.run();
			}
		});

		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				alertDialog.dismiss();
			}
		}); 

		alertDialog.show();
	}

	public static int deleteBookAlert(Context context, final CatalogueDBAdapter dbHelper, final long id, final Runnable onDeleted) {

		ArrayList<Author> authorList = dbHelper.getBookAuthorList(id);

		String title;
		Cursor cur = dbHelper.fetchBookById(id);
		try {
			if (cur == null || !cur.moveToFirst())
				return R.string.unable_to_find_book;

			title = cur.getString(cur.getColumnIndex(CatalogueDBAdapter.KEY_TITLE));
			if (title == null || title.length() == 0)
				title = "<Unknown>";
			
		} finally {
			if (cur != null)
				cur.close();
		}

		// Format the list of authors nicely
		String authors;
		if (authorList.size() == 0)
			authors = "<Unknown>";
		else {
			authors = authorList.get(0).getDisplayName();
			for (int i = 1; i < authorList.size() - 1; i++) {
				authors += ", " + authorList.get(i).getDisplayName();
			}
			if (authorList.size() > 1)
				authors += " " + context.getResources().getString(R.string.list_and) + " " + authorList.get(authorList.size() -1).getDisplayName();
		}

		// Get the title		
		String format = context.getResources().getString(R.string.really_delete_book);
		
		String message = String.format(format, title, authors);
		final AlertDialog alertDialog = new AlertDialog.Builder(context).setMessage(message).create();

		alertDialog.setTitle(R.string.menu_delete);
		alertDialog.setIcon(android.R.drawable.ic_menu_info_details);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dbHelper.deleteBook(id);
				alertDialog.dismiss();
				onDeleted.run();
			}
		});

		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				alertDialog.dismiss();
			}
		}); 

		alertDialog.show();
		return 0;
		
	}

	/**
	 * Display a dialog warning the user that goodreads authentication is required; gives them
	 * the options: 'request now', 'more info' or 'cancel'.
	 */
	public static int goodreadsAuthAlert(final Context context) {
		// Get the title		
		final AlertDialog alertDialog = new AlertDialog.Builder(context).setTitle(R.string.authorize_access).setMessage(R.string.goodreads_action_cannot_blah_blah).create();

		alertDialog.setIcon(android.R.drawable.ic_menu_info_details);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				alertDialog.dismiss();
				GoodreadsRegister.requestAuthorization(context);
			}
		});
		
		alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.tell_me_more), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				alertDialog.dismiss();
				Intent i = new Intent(context, GoodreadsRegister.class);
				context.startActivity(i);				
			}
		});

		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				alertDialog.dismiss();
			}
		}); 

		alertDialog.show();
		return 0;
		
	}

	/**
	 * Interface for item that displays in a custom dialog list
	 */
	public static interface SimpleDialogItem {
		View getView(LayoutInflater inflater);
	}
	/**
	 * Interface to listen for item selection in a custom dialog list
	 */
	public static interface SimpleDialogOnClickListener {
		void onClick(SimpleDialogItem item);
	}

	/**
	 * Select a custom item from a list, and call halder when/if item is selected.
	 */
	public static void selectItemDialog(LayoutInflater inflater, String message, ArrayList<SimpleDialogItem> items, final SimpleDialogOnClickListener handler) {
		// Get the view and the radio group
		View root = inflater.inflate(R.layout.select_list_dialog, null);
		TextView msg = (TextView) root.findViewById(R.id.message);

		// Build the base dialog
		final AlertDialog.Builder builder = new AlertDialog.Builder(inflater.getContext()).setView(root);
		if (message != null && !message.equals("")) {
			msg.setText(message);
		} else {
			msg.setVisibility(View.GONE);
		}

		final AlertDialog dialog = builder.create();

		// Create the listener for each item
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				SimpleDialogItem item = (SimpleDialogItem)ViewTagger.getTag(v, R.id.TAG_DIALOG_ITEM);
				handler.onClick(item);				
			}};		

		// Add the items to the dialog
		LinearLayout list = (LinearLayout)root.findViewById(R.id.list);
		for(SimpleDialogItem item: items) {
			View v = item.getView(inflater);
			ViewTagger.setTag(v, R.id.TAG_DIALOG_ITEM, item);
			list.addView(v);
			v.setOnClickListener(listener);
		}
		dialog.show();
	}

	/**
	 * Wrapper class to present a list of files for selection
	 */
	public static void selectFileDialog(LayoutInflater inflater, String title, ArrayList<File> files, final SimpleDialogOnClickListener handler) {
		ArrayList<SimpleDialogItem> items = new ArrayList<SimpleDialogItem>();
		for(File file: files) {
			items.add(new SimpleDialogFileItem(file));
		}
		selectItemDialog(inflater, title, items, handler);
	}

	/**
	 * Simple item to manage a File object in a list of items.
	 */
	public static class SimpleDialogFileItem implements SimpleDialogItem {
		private final File mFile; 
		
		public SimpleDialogFileItem(File file) {
			mFile = file;
		}
		
		public File getFile() {
			return mFile;
		}

		/**
		 * Get a View to display the file
		 */
		public View getView(LayoutInflater inflater) {
			// Create the view
			View v = inflater.inflate(R.layout.file_list_item, null);
			// Set the file name
			TextView name = (TextView) v.findViewById(R.id.name);
			name.setText(mFile.getName());
			// Set the path
			TextView location = (TextView) v.findViewById(R.id.path);
			location.setText(mFile.getParent());
			// Set the size
			TextView size = (TextView) v.findViewById(R.id.size);
			size.setText(Utils.formatFileSize(mFile.length()));
			// Set the last modified date
			TextView update = (TextView) v.findViewById(R.id.updated);
			update.setText(Utils.toPrettyDateTime(new Date(mFile.lastModified())));
			// Return it
			return v;
		}
	}
}
