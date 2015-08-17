package ru.bk.klim9.P21402musicplayer;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

    final String LOG_TAG = "myLogs";

    private RecyclerView recyclerView;
    private SeekBar seekbar;

    private Handler durationHandler = new Handler();
    private double timeElapsed = 0, finalTime = 0;

    TextView songName, author, album, duration, passTime;

    int currentPosition = 0;

    ServiceConnection sConn;
    Intent intent;
    Intent intentConnect;
    PlayService myService;
    boolean bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "PlaylistActivity onCreate 1");

        intentConnect = new Intent(this, PlayService.class);
        startService(intentConnect);

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



        intent = new Intent(this, PlayService.class);
        sConn = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(LOG_TAG, "PlaylistActivity onServiceConnected 1");
                myService = ((PlayService.MyBinder) binder).getService();
                bound = true;
                Log.d(LOG_TAG, "PlaylistActivity onServiceConnected 2");
                createIU();
                Log.d(LOG_TAG, "PlaylistActivity onServiceConnected 3");
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d(LOG_TAG, "PlaylistActivity onServiceDisconnected");
                bound = false;
            }
        };

        if(!bound){
            intentConnect = new Intent(this, PlayService.class);
            startService(intentConnect);
            bindService(intent, sConn, 0);
        }

        Log.d(LOG_TAG, "PlaylistActivity onCreate 2");
        
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "PlaylistActivity onStart ");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LOG_TAG, "PlaylistActivity onRestart ");
        createIU();
    }

    public void createIU(){
        Log.d(LOG_TAG, "PlaylistActivity createIU 1 ");
        Log.d(LOG_TAG, "PlaylistActivity createIU 2");

        if (PlayService.isInstantiated) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(LOG_TAG, "PlaylistActivity createIU 3"/* + myService.playlist.get(0).album*/);
            recyclerView.setAdapter(new PlaylistAdapter(myService.playlist, this));
        }

        if (myService.mediaPlayer.isPlaying()){
            setupUI();
        }

    }

    public void setupUI(){
        songName.setText(myService.playlist.get(myService.position).title);
        author.setText(myService.playlist.get(myService.position).artist);
        album.setText(myService.playlist.get(myService.position).album);
        int tf = Integer.parseInt(myService.playlist.get(myService.position).duration);
        duration.setText(String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes((long) tf), TimeUnit.MILLISECONDS.toSeconds((long) tf) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) tf))));

        seekbar.setProgress(0);
        seekbar.setMax(myService.mediaPlayer.getDuration());
        new Thread(this).start();

        currentPosition = myService.mediaPlayer.getCurrentPosition();
        durationHandler.postDelayed(updateSeekBarTime, 100);
        Log.d(LOG_TAG, "PlaylistActivity setupUI");
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
            myService.currentText = newText;
            myService.filteredPlaylist.clear();
            for (Song song : myService.playlist)
                if (song.contains(newText))
                    myService.filteredPlaylist.add(song);

            recyclerView.setAdapter(new PlaylistAdapter(myService.filteredPlaylist, this));
        } else {
            recyclerView.setAdapter(new PlaylistAdapter(myService.playlist, this));
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
                myService.chooseAllSongs();
                recyclerView.setAdapter(new PlaylistAdapter(myService.playlist, this));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }





    private void onStopClick() {
        myService.mediaPlayer.stop();
        setActiveSongIconPlaying(false);

    }

    private void onPauseClick() {
        myService.mediaPlayer.pause();
        setActiveSongIconPlaying(false);
    }

    private void onPlayClick() {
        myService.mediaPlayer.start();
        setActiveSongIconPlaying(true);
    }

    private void onRestartClick() throws IOException {
        Log.d(LOG_TAG, "PlaylistActivity onRestartClick 1");
        myService.onRestartServiceData();

        recyclerView.setAdapter(new PlaylistAdapter(myService.songList, this));

        setupUI();

    }

    private void setActiveSongIconPlaying(boolean isPlaying) {
        final ArrayList<Song> songList = myService.currentText.isEmpty() ?
                myService.playlist : myService.filteredPlaylist;
        myService.previousSong.setIsPlaying(isPlaying);
        recyclerView.setAdapter(new PlaylistAdapter(songList, this));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.song_container:
                Log.d(LOG_TAG, "PlaylistActivity onClick song_container 1");
                myService.position = recyclerView.getChildLayoutPosition(v);
                try {
                    onRestartClick();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(LOG_TAG, "PlaylistActivity onClick song_container 2");
                break;
            case R.id.fb_button:
                try {
                    if (myService.position > 0) {
                        myService.position = myService.position - 1;
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
                if (myService.mediaPlayer.isPlaying()) {
                    onPauseClick();
                } else {
                    onPlayClick();
                }
                break;
            case R.id.ff_button:
                try {
                    if (myService.position < myService.listLength - 1) {
                        myService.position = myService.position + 1;
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
        if (fromUser) myService.mediaPlayer.seekTo(progress);

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
        int total = myService.mediaPlayer.getDuration();
        while (myService.mediaPlayer != null && currentPosition < total) {
            try {
                Thread.sleep(1000);
                currentPosition = myService.mediaPlayer.getCurrentPosition();

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
            timeElapsed = myService.mediaPlayer.getCurrentPosition();
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


        Collections.sort(myService.playlist, durationComparator);
        recyclerView.setAdapter(new PlaylistAdapter(myService.playlist, this));

    }

    static Comparator<Song> titleComparator = new Comparator<Song>() {

        public int compare(Song o1, Song o2) {
            return o1.title.compareTo(o2.title);
        }
    };

    public void sortTitle() {


        Collections.sort(myService.playlist, titleComparator);
        recyclerView.setAdapter(new PlaylistAdapter(myService.playlist, this));

    }

    static Comparator<Song> artistComparator = new Comparator<Song>() {

        public int compare(Song o1, Song o2) {
            return o1.artist.compareTo(o2.artist);
        }
    };

    public void sortArtist() {


        Collections.sort(myService.playlist, artistComparator);
        recyclerView.setAdapter(new PlaylistAdapter(myService.playlist, this));

    }

    static Comparator<Song> albumComparator = new Comparator<Song>() {

        public int compare(Song o1, Song o2) {
            return o1.album.compareTo(o2.album);
        }
    };

    public void sortAlbum() {


        Collections.sort(myService.playlist, albumComparator);
        recyclerView.setAdapter(new PlaylistAdapter(myService.playlist, this));

    }


    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);


    }


    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);



    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


}
