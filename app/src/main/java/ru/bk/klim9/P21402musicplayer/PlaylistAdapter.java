package ru.bk.klim9.P21402musicplayer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.SongViewHolder> {

    private final ArrayList<Song> playlist;
    private final View.OnClickListener listener;

    public PlaylistAdapter(ArrayList<Song> playlist, View.OnClickListener listener) {
        this.playlist = playlist;
        this.listener = listener;
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View view = LayoutInflater
                .from(viewGroup.getContext())
                .inflate(R.layout.item_song, viewGroup, false);
        return new SongViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(SongViewHolder songViewHolder, int i) {
        final Song song = playlist.get(i);
        songViewHolder.setIsPlaying(song.isPlaying());
        songViewHolder.setTitle(song.title);
        songViewHolder.setArtistAndAlbum(song.artist, song.album);
        songViewHolder.setDuration(song.duration);

    }

    @Override
    public int getItemCount() {
        return playlist.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView artist_album;
        private TextView duration;
        private ImageView playStatus;

        public SongViewHolder(View itemView, View.OnClickListener listener) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            artist_album = (TextView) itemView.findViewById(R.id.artist_album);
            duration = (TextView) itemView.findViewById(R.id.duration);
            playStatus = (ImageView) itemView.findViewById(R.id.play_status_image);
            itemView.setOnClickListener(listener);
        }

        public void setTitle(String title) {
            this.title.setText(title);
        }

        public void setArtistAndAlbum(String artist, String album) {
            this.artist_album.setText(artist + " - " + album);
        }

        public void setDuration(String millisStr) {
            final long millis = Long.valueOf(millisStr);
            final String duration = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
            this.duration.setText(duration);
        }

        public void setIsPlaying(boolean isPlaying) {
            if (isPlaying)
                playStatus.setImageResource(R.drawable.ic_action_av_play_arrow);
            else
                playStatus.setImageResource(R.drawable.ic_note);
        }
    }
}
