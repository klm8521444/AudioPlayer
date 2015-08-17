package ru.bk.klim9.P21402musicplayer;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class PlayService extends Service {

    final String LOG_TAG = "myLogs";

    MyBinder binder = new MyBinder();
    public static boolean isInstantiated = false;

    ArrayList<Song> playlist;
    ArrayList<Song> songList;
    ArrayList<Song> filteredPlaylist;
    int listLength;
    MediaPlayer mediaPlayer;
    Song previousSong;
    String currentText = "";
    int position;


    public PlayService() {
    }

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "MyService onCreate");

        filteredPlaylist = new ArrayList<>();
        mediaPlayer = new MediaPlayer();

    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "MyService onDestroy");

    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(LOG_TAG, "MyService onStartCommand0");

        if (intent.getStringArrayExtra("paths") != null) {
            String[] filteredPaths = intent.getStringArrayExtra("paths");
            Log.d(LOG_TAG, "MyService onStartCommand1");
            listLength = listLength + filteredPaths.length;
            Log.d(LOG_TAG, "MyService onStartCommand2");

            playlist = new ArrayList<>();
            playlist.clear();
            for (int i = 0; i < filteredPaths.length; i++) {
                playlist.add(new Song(filteredPaths[i]));
            }
            isInstantiated = true;
            Log.d(LOG_TAG, "MyService onStartCommand 3" + playlist.get(0).album);
        }

        return super.onStartCommand(intent, flags, startId);

    }


    public IBinder onBind(Intent arg0) {
        //isInstantiated = true;
        Log.d(LOG_TAG, "MyService onBind");
        return binder;
    }

    class MyBinder extends Binder {
        PlayService getService() {
            return PlayService.this;
        }
    }

    protected void chooseAllSongs() {
        playlist = new ArrayList<>();
        isInstantiated = true;

        final String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        final String[] projection = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
        };
        final String sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC";

        Cursor cursor = null;
        try {
            Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            cursor = getContentResolver().query(uri, projection, selection, null, sortOrder);
            if (cursor != null) {
                cursor.moveToFirst();
                playlist.clear();
                while (!cursor.isAfterLast()) {
                    final String path = cursor.getString(0);
                    final String title = cursor.getString(1);
                    final String artist = cursor.getString(2);
                    final String album = cursor.getString(3);
                    final String duration = cursor.getString(4);
                    playlist.add(new Song(path, title, artist, album, duration));

                    cursor.moveToNext();
                }
                //recyclerView.setAdapter(new PlaylistAdapter(playlist, this));
            }
        } catch (Exception e) {
            Log.e("MusicPlayer", e.toString());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void onRestartServiceData() throws IOException {
        Log.d(LOG_TAG, "MyService onRestartServiceData 1 " + position);
        songList = currentText.isEmpty() ?
                playlist : filteredPlaylist;
        final Song song = songList.get(position);
        if (previousSong != null)
            previousSong.setIsPlaying(false);
        previousSong = song;
        song.setIsPlaying(true);

        mediaPlayer.stop();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(song.path);
        mediaPlayer.prepare();
        mediaPlayer.start();

    }

}
