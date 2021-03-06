package com.team11.hackernews;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.team11.hackernews.api.data.AskHN;
import com.team11.hackernews.api.data.Job;
import com.team11.hackernews.api.data.Poll;
import com.team11.hackernews.api.data.Story;
import com.team11.hackernews.api.data.Thread;

import java.net.URL;
import java.util.ArrayList;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    public static final String THREAD_LIST_KEY = "THREAD_LIST";
    public static final String ITEM_ADAPTER_KEY = "ITEM_ADAPTER";

    Callbacks mCallbacks;
    ItemInteractionCallbacks mItemInteractionCallbacks;
    private ArrayList<Thread> mItemArrayList;
    private int mMaxBinded;
    private Context mContext;

    public ItemAdapter(Context context, Callbacks callbacks, ItemInteractionCallbacks itemInteractionCallbacks) {
        mItemArrayList = new ArrayList<Thread>();
        mMaxBinded = 0;
        mCallbacks = callbacks;
        mItemInteractionCallbacks = itemInteractionCallbacks;
        mContext = context;
    }

    public void saveState(Bundle outState) {
        Bundle itemAdapterBundle = new Bundle();
        itemAdapterBundle.putParcelableArrayList(THREAD_LIST_KEY, mItemArrayList);
        outState.putBundle(ITEM_ADAPTER_KEY, itemAdapterBundle);
    }

    public void restoreState(Bundle inState) {
        Bundle itemAdapterBundle = inState.getBundle(ITEM_ADAPTER_KEY);
        mItemArrayList = itemAdapterBundle.getParcelableArrayList(THREAD_LIST_KEY);
    }

    public void add(Thread item) {
        mItemArrayList.add(item);
    }

    public void clear() {
        mItemArrayList.clear();
        mMaxBinded = 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        // create a new view
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_main_row_layout, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        Log.d("itemCount-1 = ", String.valueOf(getItemCount() - 1));
        Log.d("mMaxBinded", String.valueOf(mMaxBinded));
        Log.d("max item's id", String.valueOf(mItemArrayList.get(getItemCount() - 1).getId()));
        if (mMaxBinded < i) {
            mMaxBinded = i;
            if (mMaxBinded == this.getItemCount() - 1) {
                Log.d("ItemAdapter", "Reached bottom");
                mCallbacks.onReachedBottom();
            }
        }

        Thread currentItem = mItemArrayList.get(i);

        viewHolder.mTitle.setText(currentItem.getTitle());
        viewHolder.mScore.setText(currentItem.getScore().toString());
        viewHolder.mBy.setText(currentItem.getBy());
        CharSequence timeAgo = getTime(currentItem.getTime());
        viewHolder.mTime.setText(timeAgo);
        String domainString;
        String commentsString;

        // Domain
        if (currentItem instanceof Story) {
            URL UrlObject = ((Story) currentItem).getURL();

            String host = UrlObject.getHost();
            domainString = host.startsWith("www.") ? host.substring(4) : host;
            domainString = "(" + domainString + ")";
        } else if (currentItem instanceof Job) {
            try {
                URL UrlObject = ((Job) currentItem).getURL();
                String host = UrlObject.getHost();
                domainString = host.startsWith("www.") ? host.substring(4) : host;
                domainString = "(" + domainString + ")";
            } catch (Exception e) {
                // Some job postings do not link to external site
                domainString = "";
            }
        } else if (currentItem instanceof Poll) {
            domainString = "User Poll";
        } else if (currentItem instanceof AskHN) {
            String title = currentItem.getTitle();
            if (title.startsWith("Ask HN:")) {
                domainString = "Ask HN";
            } else {
                domainString = "Show HN";
            }
        } else {
            domainString = "";
        }

        // Comments
        if (currentItem instanceof Job) {
            commentsString = "Job Posting";
        } else {
            commentsString = currentItem.getKids().size() + " comments";
        }

        viewHolder.mDomain.setText(domainString);
        viewHolder.mComments.setText(commentsString);

        if (currentItem instanceof Story) {
            //Stories require both a URL on title and link to comments
            viewHolder.mTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = ((Story) mItemArrayList.get(i)).getURL().toString();
                    mItemInteractionCallbacks.loadWebView(url);
                }

            });
            viewHolder.mComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemInteractionCallbacks.loadCommentsView(mItemArrayList.get(i));
                }
            });
        } else if (currentItem instanceof Job) {
            //Jobs have no comments and a URL link in the title
            viewHolder.mTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = ((Job) mItemArrayList.get(i)).getURL().toString();
                    mItemInteractionCallbacks.loadWebView(url);
                }
            });
            viewHolder.mComments.setOnClickListener(null);
        } else if ((currentItem instanceof AskHN) || (currentItem instanceof Poll)) {
            //AskHN and Polls both should open comments when either Title or Comments is pressed
            viewHolder.mTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemInteractionCallbacks.loadCommentsView(mItemArrayList.get(i));
                }
            });
            viewHolder.mComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mItemInteractionCallbacks.loadCommentsView(mItemArrayList.get(i));
                }
            });
        }

        if (isThreadFavorite(currentItem.getId())) {
            viewHolder.mFavorite.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_important));
        } else {
            viewHolder.mFavorite.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_not_important));
        }
        viewHolder.mFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Long id = mItemArrayList.get(i).getId();
                if (isThreadFavorite(id)) {
                    Toast.makeText(ItemAdapter.this.mContext, "Post removed from favourites", Toast.LENGTH_LONG).show();
                    ((ImageView) v).setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_not_important));
                    String where = "";
                    String[] whereArgs = {String.valueOf(id)};
                    mContext.getContentResolver().delete(WatchedThreadsContentProvider.CONTENT_URI, where, whereArgs);
                } else {
                    Toast.makeText(ItemAdapter.this.mContext, "Post saved in favourites", Toast.LENGTH_LONG).show();
                    ((ImageView) v).setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_important));
                    ContentValues cv = new ContentValues();
                    cv.put("id", mItemArrayList.get(i).getId().toString());
                    mContext.getContentResolver().insert(WatchedThreadsContentProvider.CONTENT_URI, cv);
                }
            }
        });
    }

    //move to content resolver?
    private boolean isThreadFavorite(Long id){
        String[] projection = {WatchedThreadsOpenHelper.KEY_THREAD_ID};
        String selection = WatchedThreadsOpenHelper.KEY_THREAD_ID+"=?";
        String[] selectionArgs = {String.valueOf(id)};
        Cursor cursor = mContext.getContentResolver().query(WatchedThreadsContentProvider.CONTENT_URI, projection,
                selection, selectionArgs, null);
        boolean isFavorite = cursor.getCount() == 1;
        cursor.close();
        return isFavorite;
    }

    private CharSequence getTime(long timeStamp) {
        timeStamp = timeStamp * 1000;
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(timeStamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        return timeAgo;
    }

    @Override
    public int getItemCount() {
        return mItemArrayList.size();
    }

    public interface Callbacks {
        public void onReachedBottom();
    }

    public interface ItemInteractionCallbacks {
        public void loadWebView(String url);
        public void loadCommentsView(Thread thread);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout mTextView;
        public TextView mTitle;
        public TextView mScore;
        public TextView mBy;
        public TextView mTime;
        public TextView mDomain;
        public TextView mComments;
        public ImageView mFavorite;

        public ViewHolder(LinearLayout v) {
            // each data item is just a string in this case
            super(v);
            mTextView = v;
            mTitle = (TextView) v.findViewById(R.id.mTitle);
            mScore = (TextView) v.findViewById(R.id.mScore);
            mBy = (TextView) v.findViewById(R.id.mBy);
            mTime = (TextView) v.findViewById(R.id.mTime);
            mDomain = (TextView) v.findViewById(R.id.mDomain);
            mComments = (TextView) v.findViewById(R.id.mComments);
            mFavorite = (ImageView) v.findViewById(R.id.mFavorite);
        }
    }
}
