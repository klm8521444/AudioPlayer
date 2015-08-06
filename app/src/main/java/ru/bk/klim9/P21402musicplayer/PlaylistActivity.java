package ru.bk.klim9.P21402musicplayer;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class PlaylistActivity extends BaseActivity implements Runnable, View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, SearchView.OnQueryTextListener {

    private RecyclerView recyclerView;
    private ArrayList<Song> playlist;
    private ArrayList<Song> filteredPlaylist;
    public MediaPlayer mediaPlayer;
    private SeekBar seekbar;
    private String currentText = "";
    private Song previousSong;

    private Handler durationHandler = new Handler();
    private double timeElapsed = 0, finalTime = 0;


    private int position = 0;
    private int listLength = 0;

    TextView songName, author, album, duration, passTime;

    int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findViewById(R.id.fb_button).setOnClickListener(this);
        findViewById(R.id.stop_button).setOnClickListener(this);
        findViewById(R.id.play_button).setOnClickListener(this);
        findViewById(R.id.ff_button).setOnClickListener(this);

        songName = (TextView) findViewById(R.id.songName);
        author = (TextView) findViewById(R.id.author);
        album = (TextView) findViewById(R.id.album);
        duration = (TextView) findViewById(R.id.songDuration);
        passTime = (TextView) findViewById(R.id.passTime);

        seekbar = (SeekBar) findViewById(R.id.seekBar);

        seekbar.setOnSeekBarChangeListener(this);


        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(
                this,
                LinearLayoutManager.VERTICAL,
                false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        playlist = new ArrayList<>();
        filteredPlaylist = new ArrayList<>();
        mediaPlayer = new MediaPlayer();



    }

    @Override
    protected int getLayoutResourceIdentifier() {
        return R.layout.activity_playlist;
    }

    @Override
    protected String getTitleToolBar() {
        return getString(R.string.app_name);
    }

    @Override
    protected boolean getDisplayHomeAsUp() {
        return false;
    }

    @Override
    protected boolean getHomeButtonEnabled() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_playlist_activity, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setSubmitButtonEnabled(false);
            searchView.setOnQueryTextListener(this);
            searchView.setQueryHint("Search");
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (!newText.isEmpty()) {
            currentText = newText;
            filteredPlaylist.clear();
            for (Song song : playlist)
                if (song.contains(newText))
                    filteredPlaylist.add(song);

            recyclerView.setAdapter(new PlaylistAdapter(filteredPlaylist, this));
        } else {
            recyclerView.setAdapter(new PlaylistAdapter(playlist, this));
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startDirectoryChoosActivity();
                break;
            case R.id.action_all:
                chooseAllSongs();
                break;
            case R.id.sortTitle:
                sortTitle();
                break;
            case R.id.sortArtist:
                sortArtist();
                break;
            case R.id.sortAlbum:
                sortAlbum();
                break;
            case R.id.sortTime:
                sortTime();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startDirectoryChoosActivity() {
        Intent intent = new Intent(this, DirectoryChoosActivity.class);
        startActivityForResult(intent, 1);

    }

    private void chooseAllSongs() {
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
                recyclerView.setAdapter(new PlaylistAdapter(playlist, this));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        String[] filteredPaths = data.getStringArrayExtra("pathsArray");
        listLength = listLength + filteredPaths.length;
        playlist.clear();
        for (int i = 0; i < filteredPaths.length; i++) {
            playlist.add(new Song(filteredPaths[i]));
        }
        recyclerView.setAdapter(new PlaylistAdapter(playlist, this));
    }


    private void onStopClick() {
        mediaPlayer.stop();
        setActiveSongIconPlaying(false);
    }

    private void onPauseClick() {
        mediaPlayer.pause();
        setActiveSongIconPlaying(false);
    }

    private void onPlayClick() {
        mediaPlayer.start();
        setActiveSongIconPlaying(true);
    }

    private void onRestartClick() throws IOException {
        final ArrayList<Song> songList = currentText.isEmpty() ?
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

        recyclerView.setAdapter(new PlaylistAdapter(songList, this));

        songName.setText(playlist.get(position).title);
        author.setText(playlist.get(position).artist);
        album.setText(playlist.get(position).album);
        int tf = Integer.parseInt(playlist.get(position).duration);
        duration.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes((long) tf), TimeUnit.MILLISECONDS.toSeconds((long) tf) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) tf))));

        seekbar.setProgress(0);
        seekbar.setMax(mediaPlayer.getDuration());
        new Thread(this).start();

        currentPosition = mediaPlayer.getCurrentPosition();
        durationHandler.postDelayed(updateSeekBarTime, 100);
    }

    private void setActiveSongIconPlaying(boolean isPlaying) {
        final ArrayList<Song> songList = currentText.isEmpty() ?
                playlist : filteredPlaylist;
        previousSong.setIsPlaying(isPlaying);
        recyclerView.setAdapter(new PlaylistAdapter(songList, this));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.song_container:
                position = recyclerView.getChildLayoutPosition(v);
                try {
                    onRestartClick();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.fb_button:
                try {
                    if (position > 0) {
                        position = position - 1;
                    }
                    onRestartClick();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.stop_button:
                onStopClick();
                break;
            case R.id.play_button:
                if (mediaPlayer.isPlaying()) {
                    onPauseClick();
                } else {
                    onPlayClick();
                }
                break;
            case R.id.ff_button:
                try {
                    if (position < listLength - 1) {
                        position = position + 1;
                    }
                    onRestartClick();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (fromUser) mediaPlayer.seekTo(progress);

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void run() {
        //int currentPosition = 0;
        int total = mediaPlayer.getDuration();
        while (mediaPlayer != null && currentPosition < total) {
            try {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();

            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                return;
            }
            seekbar.setProgress(currentPosition);


        }
    }

    //handler to change seekBarTime
    private Runnable updateSeekBarTime = new Runnable() {
        public void run() {
            //get current position
            timeElapsed = mediaPlayer.getCurrentPosition();
            //set seekbar progress
            seekbar.setProgress((int) timeElapsed);

            //set time remaing
            double timeRemaining = finalTime - timeElapsed;
            passTime.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining), TimeUnit.MILLISECONDS.toSeconds((long) timeRemaining) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) timeRemaining))));

            //repeat yourself that again in 100 miliseconds
            durationHandler.postDelayed(this, 100);
        }
    };

    static Comparator<Song> durationComparator = new Comparator<Song>() {

        public int compare(Song o1, Song o2) {
            return o1.duration.compareTo(o2.duration);
        }
    };

    public void sortTime() {


        Collections.sort(playlist, durationComparator);
        recyclerView.setAdapter(new PlaylistAdapter(playlist, this));

    }

    static Comparator<Song> titleComparator = new Comparator<Song>() {

        public int compare(Song o1, Song o2) {
            return o1.title.compareTo(o2.title);
        }
    };

    public void sortTitle() {


        Collections.sort(playlist, titleComparator);
        recyclerView.setAdapter(new PlaylistAdapter(playlist, this));

    }

    static Comparator<Song> artistComparator = new Comparator<Song>() {

        public int compare(Song o1, Song o2) {
            return o1.artist.compareTo(o2.artist);
        }
    };

    public void sortArtist() {


        Collections.sort(playlist, artistComparator);
        recyclerView.setAdapter(new PlaylistAdapter(playlist, this));

    }

    static Comparator<Song> albumComparator = new Comparator<Song>() {

        public int compare(Song o1, Song o2) {
            return o1.album.compareTo(o2.album);
        }
    };

    public void sortAlbum() {


        Collections.sort(playlist, albumComparator);
        recyclerView.setAdapter(new PlaylistAdapter(playlist, this));

    }

    /*
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentText = savedInstanceState.getString("currentText");
        timeElapsed = savedInstanceState.getDouble("timeElapsed");
        finalTime = savedInstanceState.getDouble("finalTime");
        position = savedInstanceState.getInt("position");
        listLength = savedInstanceState.getInt("listLength");
        currentPosition = savedInstanceState.getInt("currentPosition");

    }


    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("currentText", currentText);
        outState.putDouble("timeElapsed", timeElapsed);
        outState.putDouble("finalTime", finalTime);
        outState.putInt("position", position);
        outState.putInt("listLength", listLength);
        outState.putInt("currentPosition", currentPosition);

    }
    */

    /*
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return seekbar;
    }
    */


    /*
     private RecyclerView recyclerView;
    private ArrayList<Song> playlist;
    private ArrayList<Song> filteredPlaylist;
    private MediaPlayer mediaPlayer;
    private SeekBar seekbar;
    private String currentText = "";
    private Song previousSong;

    private Handler durationHandler = new Handler();
    private double timeElapsed = 0, finalTime = 0;


    private int position = 0;
    private int listLength = 0;

    TextView songName, author, album, duration, passTime;

    int currentPosition = 0;

     */


   /*
    @Override
    protected void onRestart() {
        super.onRestart();
        mediaPlayer = (MediaPlayer) getLastNonConfigurationInstance();
    }

    public Object onRetainNonConfigurationInstance(MediaPlayer mediaPlayer) {
        return mediaPlayer;
    }
    */




    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
    }


}
