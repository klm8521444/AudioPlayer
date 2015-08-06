package ru.bk.klim9.P21402musicplayer;

import android.media.MediaMetadataRetriever;

public class Song {
    private static final MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    public int icon1;
    public final String path;
    public final String title;
    public final String artist;
    public final String album;
    public final String duration;
    private boolean isPlaying = false;

    public Song(String path) {
        this.icon1 = icon1;
        this.path = path;
        mmr.setDataSource(path);
        this.title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        this.artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        this.album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        this.duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    }

    public Song(String path, String title, String artist, String album, String duration) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.icon1 = icon1;
    }

    public boolean contains(final String subStr) {
        return title.toUpperCase().contains(subStr.toUpperCase())
                || artist.toUpperCase().contains(subStr.toUpperCase())
                || album.toUpperCase().contains(subStr.toUpperCase());
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }
}

