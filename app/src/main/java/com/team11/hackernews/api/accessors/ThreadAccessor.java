package com.team11.hackernews.api.accessors;

import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.team11.hackernews.api.HackerNewsAPI;
import com.team11.hackernews.api.Utils;
import com.team11.hackernews.api.data.AskHN;
import com.team11.hackernews.api.data.Job;
import com.team11.hackernews.api.data.Poll;
import com.team11.hackernews.api.data.Story;
import com.team11.hackernews.api.data.Thread;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadAccessor extends Accessor {

    public void getStory(final long id, final GetStoryCallbacks callbacks) {
        Utils.getFirebaseInstance().child(HackerNewsAPI.ITEM + "/" + id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mCancelPendingCallbacks) {
                    return;
                }

                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                if (map == null)
                    return;

                Boolean deleted = (Boolean) map.get("deleted");

                if ((deleted != null) && deleted) {
                    callbacks.onDeleted(id);
                    return;
                }

                Thread thread;

                Utils.ItemType itemType = Utils.getItemTypeFromString(map.get("type").toString());

                if (itemType == Utils.ItemType.Story) {
                    Log.d("Ask hn", "story id " + map.get("id"));
                    Log.d("Ask hn", String.valueOf(map.get("title")));
                    String title = map.get("title").toString();
                    if (title.startsWith("Ask HN:") || title.startsWith("Show HN:")) {
                        // Ask HN thread
                        thread = new AskHN();

                    } else {

                        // Story thread
                        thread = new Story();

                        URL url = null;
                        try {
                            url = new URL((String) map.get("url"));
                        } catch (MalformedURLException ignored) {
                        }

                        ((Story) thread).setURL(url);
                    }

                } else if (itemType == Utils.ItemType.Job) {

                    // Job thread
                    thread = new Job();

                    URL url = null;
                    try {
                        url = new URL((String) map.get("url"));
                    } catch (MalformedURLException ignored) {
                        // This means that the job posting does not link to an external site
                        // so just keep the URL as null
                    }

                    ((Job) thread).setURL(url);

                } else if (itemType == Utils.ItemType.Poll) {

                    // Poll thread
                    thread = new Poll();

                    List<Long> pollOpts = (List<Long>) map.get("pollopts");
                    if (pollOpts == null) {
                        // TODO: this is when the poll has no options, possibly set the onError() to take an Error object
                        callbacks.onError(id, new Error("No Poll Options"));
                        return;
                    }

                } else {

                    callbacks.onWrongItemType(itemType, (Long) map.get("id"));
                    return;

                }

                // These fields are common to all thread types so we can parse them for whatever thread type is being requested
                List<Long> kids = (List<Long>) map.get("kids");
                if (kids == null) {
                    kids = new ArrayList<Long>();
                }

                thread.setBy((String) map.get("by"));
                thread.setId((Long) map.get("id"));
                thread.setKids(kids);
                thread.setScore((Long) map.get("score"));
                thread.setTime((Long) map.get("time"));
                thread.setText((String) map.get("text"));
                thread.setTitle((String) map.get("title"));

                callbacks.onSuccess(thread);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                if (mCancelPendingCallbacks) {
                    return;
                }
                callbacks.onError(id, new Error("Firebase error: " + firebaseError.getMessage()));
            }
        });
    }

    public void getMultipleStories(final List<Long> ids, final GetMultipleStoriesCallbacks callbacks) {

        final List<Thread> threads = new ArrayList<Thread>();

        if (ids.size() == 0) {
            callbacks.onSuccess(threads);
            return;
        }

        final HashMap<Long, Boolean> isDeleted = new HashMap<Long, Boolean>();

        for (final long id : ids) {
            getStory(id, new GetStoryCallbacks() {
                @Override
                public void onSuccess(Thread thread) {
                    if (mCancelPendingCallbacks) {
                        return;
                    }
                    threads.add(thread);
                    isDeleted.put(id, false);

                    runIfAllReturned();
                }

                @Override
                public void onDeleted(long id) {
                    if (mCancelPendingCallbacks) {
                        return;
                    }

                    isDeleted.put(id, true);
                    runIfAllReturned();
                }

                public void runIfAllReturned() {
                    if (isDeleted.size() == ids.size()) {
                        callbacks.onSuccess(threads);
                    }
                }

                @Override
                public void onWrongItemType(Utils.ItemType itemType, long id) {
                    if (mCancelPendingCallbacks) {
                        return;
                    }
                    isDeleted.put(id, false);
                    runIfAllReturned();
                }

                @Override
                public void onError(long id, Error error) {
                    if (mCancelPendingCallbacks) {
                        return;
                    }
                    isDeleted.put(id, false);
                    runIfAllReturned();
                }
            });
        }
    }


    public void cancelPendingCallbacks() {
        mCancelPendingCallbacks = true;
    }

    public interface GetStoryCallbacks {
        public void onSuccess(Thread thread);

        public void onDeleted(long id);

        public void onWrongItemType(Utils.ItemType itemType, long id);

        public void onError(long id, Error error);
    }

    public interface GetMultipleStoriesCallbacks {
        public void onSuccess(List<Thread> threads);

        public void onError();
    }
}
