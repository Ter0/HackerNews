package com.team11.hackernews.api.accessors;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.GenericTypeIndicator;
import com.firebase.client.ValueEventListener;
import com.team11.hackernews.api.HackerNewsAPI;
import com.team11.hackernews.api.Item;

import java.util.List;

public class TopStoriesAccessor extends Accessor {

    private List<Long> mStoryIds;
    private int mNextStoryIdx;
    private int mPageLength;

    public TopStoriesAccessor(int pageLength) {
        super();
        mNextStoryIdx = 0;
        mPageLength = pageLength;
    }

    public TopStoriesAccessor(int pageLength, Firebase firebase) {
        super(firebase);
        mNextStoryIdx = 0;
        mPageLength = pageLength;
    }

    private List<Long> getNextPage(){
        int endIdx = mNextStoryIdx + mPageLength;
        if (endIdx > mStoryIds.size()){
            endIdx = mStoryIds.size();
        }
        List<Long> storyIds = mStoryIds.subList(mNextStoryIdx, endIdx);
        mNextStoryIdx = endIdx;
        return storyIds;

    }

    public void getInitialStories(final GetTopStoriesCallbacks callbacks) {
        mFirebase.child(HackerNewsAPI.TOP_STORIES).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mCancelPendingCallbacks) {
                    return;
                }

                // Get all top stories in case they change order while trying to load the next page
                mStoryIds = dataSnapshot.getValue(new GenericTypeIndicator<List<Long>>() {
                });

                new ItemAccessor(mFirebase).getMultipleItems(getNextPage(), new ItemAccessor.GetMultipleItemsCallbacks() {
                    @Override
                    public void onSuccess(List<Item> items) {
                        callbacks.onSuccess(items);
                    }

                    @Override
                    public void onError() {

                    }
                });

                //callbacks.onSuccess(getNextPage());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                if (mCancelPendingCallbacks) {
                    return;
                }
                callbacks.onError();
            }
        });
    }

    public void getNextStories(final GetTopStoriesCallbacks callbacks) {
        new ItemAccessor(mFirebase).getMultipleItems(getNextPage(), new ItemAccessor.GetMultipleItemsCallbacks() {
            @Override
            public void onSuccess(List<Item> items) {
                callbacks.onSuccess(items);
            }

            @Override
            public void onError() {

            }
        });
        //callbacks.onSuccess(getNextPage());
    }

    public interface GetTopStoriesCallbacks {
        public void onSuccess(List<Item> stories);
        public void onError();
    }
}
