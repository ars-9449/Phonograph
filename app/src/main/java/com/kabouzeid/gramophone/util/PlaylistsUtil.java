package com.kabouzeid.gramophone.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.PlaylistSong;
import com.kabouzeid.gramophone.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistsUtil {

    public static int createPlaylist(@NonNull final Context context, @Nullable final String name) {
        int id = -1;
        if (name != null && name.length() > 0) {
            Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Playlists._ID}, MediaStore.Audio.PlaylistsColumns.NAME + "=?", new String[]{name}, null);
            if (cursor == null || cursor.getCount() < 1) {
                final ContentValues values = new ContentValues(1);
                values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
                final Uri uri = context.getContentResolver().insert(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        values);
                if (uri != null) {
                    Toast.makeText(context, context.getResources().getString(
                            R.string.created_playlist_x, name), Toast.LENGTH_SHORT).show();
                    id = Integer.parseInt(uri.getLastPathSegment());
                }
            } else {
                if (cursor.moveToFirst()) {
                    id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        if (id == -1) {
            Toast.makeText(context, context.getResources().getString(
                    R.string.could_not_create_playlist), Toast.LENGTH_SHORT).show();
        }
        return id;
    }

    public static void deletePlaylists(@NonNull final Context context, @NonNull final ArrayList<Playlist> playlists) {
        final Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        final StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Playlists._ID + " IN (");
        for (int i = 0; i < playlists.size(); i++) {
            selection.append(playlists.get(i).id);
            if (i < playlists.size() - 1) {
                selection.append(",");
            }
        }
        selection.append(")");
        context.getContentResolver().delete(uri, selection.toString(), null);
    }

    public static void addToPlaylist(@NonNull final Context context, final Song song, final int playlistId, final boolean showToastOnFinish) {
        List<Song> helperList = new ArrayList<>();
        helperList.add(song);
        addToPlaylist(context, helperList, playlistId, showToastOnFinish);
    }

    public static void addToPlaylist(@NonNull final Context context, @NonNull final List<Song> songs, final int playlistId, final boolean showToastOnFinish) {
        final int size = songs.size();
        final ContentResolver resolver = context.getContentResolver();
        final String[] projection = new String[]{
                "max(" + MediaStore.Audio.Playlists.Members.PLAY_ORDER + ")",
        };
        final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
        Cursor cursor = null;
        int base = 0;

        try {
            cursor = resolver.query(uri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                base = cursor.getInt(0) + 1;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        int numinserted = 0;
        for (int offSet = 0; offSet < size; offSet += 1000)
            numinserted += resolver.bulkInsert(uri, makeInsertItems(songs, offSet, 1000, base));

        if (showToastOnFinish) {
            Toast.makeText(context, context.getResources().getString(
                    R.string.inserted_x_songs_into_playlist, numinserted), Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    public static ContentValues[] makeInsertItems(@NonNull final List<Song> songs, final int offset, int len, final int base) {
        if (offset + len > songs.size()) {
            len = songs.size() - offset;
        }

        ContentValues[] contentValues = new ContentValues[len];

        for (int i = 0; i < len; i++) {
            contentValues[i] = new ContentValues();
            contentValues[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset + i);
            contentValues[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs.get(offset + i).id);
        }
        return contentValues;
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final Song song, int playlistId) {
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
                "external", playlistId);
        String selection = MediaStore.Audio.Playlists.Members.AUDIO_ID + " =?";
        String[] selectionArgs = new String[]{String.valueOf(song.id)};

        context.getContentResolver().delete(uri, selection, selectionArgs);
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final List<PlaylistSong> songs) {
        final int playlistId = songs.get(0).playlistId;
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(
                "external", playlistId);
        String selectionArgs[] = new String[songs.size()];
        for (int i = 0; i < selectionArgs.length; i++) {
            selectionArgs[i] = String.valueOf(songs.get(i).idInPlayList);
        }
        String selection = MediaStore.Audio.Playlists.Members._ID + " in (";
        for (String selectionArg : selectionArgs) selection += "?, ";
        selection = selection.substring(0, selection.length() - 2) + ")";

        context.getContentResolver().delete(uri, selection, selectionArgs);
    }

    public static boolean doPlaylistContains(@NonNull final Context context, final long playlistId, final int songId) {
        if (playlistId != -1) {
            Cursor c = context.getContentResolver().query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                    new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID}, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?", new String[]{String.valueOf(songId)}, null);
            int count = 0;
            if (c != null) {
                count = c.getCount();
                c.close();
            }
            return count > 0;
        }
        return false;
    }

    public static boolean moveItem(@NonNull final Context context, int playlistId, int from, int to) {
        return MediaStore.Audio.Playlists.Members.moveItem(context.getContentResolver(),
                playlistId, from, to);
    }

    public static void renamePlaylist(@NonNull final Context context, final long id, final String newName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Audio.PlaylistsColumns.NAME, newName);
        context.getContentResolver().update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                contentValues,
                MediaStore.Audio.Playlists._ID + "=?",
                new String[]{String.valueOf(id)});
    }

    public static String getNameForPlaylist(@NonNull final Context context, final long id) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.PlaylistsColumns.NAME},
                BaseColumns._ID + "=?",
                new String[]{String.valueOf(id)},
                null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return "";
    }
}