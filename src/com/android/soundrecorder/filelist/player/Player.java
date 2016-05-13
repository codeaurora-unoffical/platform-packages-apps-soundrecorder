/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.soundrecorder.filelist.player;

import android.media.MediaPlayer;
import android.os.Handler;
import android.view.View;

import com.android.soundrecorder.filelist.listitem.BaseListItem;
import com.android.soundrecorder.filelist.listitem.MediaItem;

import java.io.IOException;

public class Player implements MediaPlayer.OnCompletionListener {
    private PlayerPanel mPlayerPanel;
    private MediaItem mMediaItem;
    private MediaPlayer mPlayer = null;

    private static final int UPDATE_FREQ = 1000;
    private final Handler mHandler = new Handler();
    private Runnable mUpdateProgress = new Runnable() {
        public void run() {
            updateUi();
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            stopPlayer();
            return true;
        }
    };

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopPlayer();
    }

    public Player(PlayerPanel playerPanel) {
        mPlayerPanel = playerPanel;
        mPlayerPanel.setPlayerButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer == null) {
                    startPlayer();
                } else if (mPlayer.isPlaying()) {
                    pausePlayer();
                } else {
                    resumePlayer();
                }
            }
        });
    }

    public void setPlayerPanelLayoutListener(PlayerPanel.LayoutChangedListener listener) {
        if (mPlayerPanel != null) {
            mPlayerPanel.setLayoutChangedListener(listener);
        }
    }

    public void setItem(MediaItem item) {
        if (mMediaItem != null) {
            mMediaItem.setPlayStatus(MediaItem.PlayStatus.NONE);
        }
        mMediaItem = item;
        resetUi();
    }

    public MediaItem getMediaItem() {
        return mMediaItem;
    }

    public boolean isItemUsing(BaseListItem item) {
        if (item instanceof MediaItem) {
            return item.equals(mMediaItem);
        }
        return false;
    }

    private void updateUi() {
        if (mPlayerPanel != null && mPlayer != null) {
            mPlayerPanel.updateTitle(mMediaItem.getTitle());
            mPlayerPanel.updateProgress(mPlayer.getCurrentPosition(), mMediaItem.getDuration());
            boolean isPlaying = mPlayer.isPlaying();
            mPlayerPanel.updatePlayerStatus(isPlaying);
            if (isPlaying) {
                mHandler.postDelayed(mUpdateProgress, UPDATE_FREQ);
                mMediaItem.setPlayStatus(MediaItem.PlayStatus.PLAYING);
            } else {
                mMediaItem.setPlayStatus(MediaItem.PlayStatus.PAUSE);
            }
        }
    }

    private void resetUi() {
        mMediaItem.setPlayStatus(MediaItem.PlayStatus.PAUSE);
        if (mPlayerPanel != null) {
            mPlayerPanel.updateTitle(mMediaItem.getTitle());
            mPlayerPanel.updateProgress(0, mMediaItem.getDuration());
            mPlayerPanel.updatePlayerStatus(false);
        }
    }

    public void startPlayer() {
        stopPlayer();

        if (mPlayerPanel != null) {
            mPlayerPanel.setVisibility(View.VISIBLE);
        }

        mPlayer = new MediaPlayer();

        try {
            mPlayer.setDataSource(mMediaItem.getPath());
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(mErrorListener);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            mPlayer = null;
            return;
        }

        updateUi();
    }

    public void pausePlayer() {
        if (mPlayer == null) return;
        mPlayer.pause();
        updateUi();
    }

    private void resumePlayer() {
        if (mPlayer == null) return;
        mPlayer.start();
        updateUi();
    }

    public void stopPlayer() {
        if (mPlayer == null) return;
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
        resetUi();
    }

    public void destroyPlayer() {
        stopPlayer();
        hidePlayer();
    }

    protected void hidePlayer() {
        if (mMediaItem != null) {
            mMediaItem.setPlayStatus(MediaItem.PlayStatus.NONE);
        }
        if (mPlayerPanel != null) {
            mPlayerPanel.setVisibility(View.GONE);
        }
    }

    public boolean isPlayShown() {
        return (mPlayerPanel != null && mPlayerPanel.getVisibility() == View.VISIBLE);
    }

    public View getPlayerPanel() {
        return mPlayerPanel;
    }

    public void onItemDeleted() {
        stopPlayer();
        mMediaItem = null;
        hidePlayer();
    }

    public void onItemChanged(MediaItem newItem) {
        mMediaItem = newItem;
        updateUi();
    }
}
