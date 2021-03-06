package io.fluentic.ubicdn.fragments.list;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import io.fluentic.ubicdn.extractor.ListExtractor;
import io.fluentic.ubicdn.extractor.ListInfo;
import io.fluentic.ubicdn.extractor.stream.StreamInfoItem;
import io.fluentic.ubicdn.extractor.stream.StreamType;
import io.fluentic.ubicdn.util.Constants;

import icepick.State;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseListInfoFragment<I extends ListInfo> extends BaseListFragment<I, ListExtractor.NextItemsResult> {

    @State
    protected int serviceId = Constants.NO_SERVICE_ID;
    @State
    protected String name;
    @State
    protected String url;

    protected I currentInfo;
    protected String currentNextItemsUrl;
    protected Disposable currentWorker;

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        setTitle(name);
        showListFooter(hasMoreItems());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentWorker != null) currentWorker.dispose();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if it was loading when the fragment was stopped/paused,
        if (wasLoading.getAndSet(false)) {
            if (hasMoreItems() && infoListAdapter.getItemsList().size() > 0) {
                loadMoreItems();
            } else {
                doInitialLoadLogic();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentWorker != null) currentWorker.dispose();
        currentWorker = null;
    }

    /*//////////////////////////////////////////////////////////////////////////
    // State Saving
    //////////////////////////////////////////////////////////////////////////

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        super.writeTo(objectsToSave);
        objectsToSave.add(currentInfo);
        objectsToSave.add(currentNextItemsUrl);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
        super.readFrom(savedObjects);
        currentInfo = (I) savedObjects.poll();
        currentNextItemsUrl = (String) savedObjects.poll();
    }*/

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    public void setTitle(String title) {
        Log.d(TAG, "setTitle() called with: title = [" + title + "]");
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(title);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Load and handle
    //////////////////////////////////////////////////////////////////////////*/

    protected void doInitialLoadLogic() {
        if (DEBUG) Log.d(TAG, "doInitialLoadLogic() called");
        if (currentInfo == null) {
            startLoading(false);
        } else handleResult(currentInfo);
    }

    /**
     * Implement the logic to load the info from the network.<br/>
     * You can use the default implementations from {@link io.fluentic.ubicdn.util.ExtractorHelper}.
     *
     * @param forceLoad allow or disallow the result to come from the cache
     */
    protected abstract Single<I> loadResult(boolean forceLoad);

    @Override
    public void startLoading(boolean forceLoad) {
        //super.startLoading(forceLoad);
        /*StreamInfoItem item = new StreamInfoItem();
        item.stream_type = StreamType.VIDEO_STREAM;
        item.name = "Video 1";
        item.url = "url";
        item.service_id=0;
        item.thumbnail_url="url2";
        infoListAdapter.addInfoItem(item);*/
        showListFooter(false);
        /*currentInfo = null;
        if (currentWorker != null) currentWorker.dispose();
        currentWorker = loadResult(forceLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<I>() {
                    @Override
                    public void accept(@NonNull I result) throws Exception {
                        isLoading.set(false);
                        currentInfo = result;
                        currentNextItemsUrl = result.next_streams_url;
                        handleResult(result);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        onError(throwable);
                    }
                });*/
    }

    /**
     * Implement the logic to load more items<br/>
     * You can use the default implementations from {@link io.fluentic.ubicdn.util.ExtractorHelper}
     */
    protected abstract Single<ListExtractor.NextItemsResult> loadMoreItemsLogic();

    protected void loadMoreItems() {
        isLoading.set(true);

        if (currentWorker != null) currentWorker.dispose();
        currentWorker = loadMoreItemsLogic()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ListExtractor.NextItemsResult>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull ListExtractor.NextItemsResult nextItemsResult) throws Exception {
                        isLoading.set(false);
                        handleNextItems(nextItemsResult);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                        isLoading.set(false);
                        onError(throwable);
                    }
                });
    }

    @Override
    public void handleNextItems(ListExtractor.NextItemsResult result) {
        super.handleNextItems(result);
        currentNextItemsUrl = result.nextItemsUrl;
        infoListAdapter.addInfoItemList(result.nextItemsList);

        showListFooter(hasMoreItems());
    }

    @Override
    protected boolean hasMoreItems() {
        return !TextUtils.isEmpty(currentNextItemsUrl);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Contract
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void handleResult(@NonNull I result) {
        super.handleResult(result);
        Log.d(TAG,"Load new result "+result.name);
        url = result.url;
        name = result.name;
        setTitle(name);

        if (infoListAdapter.getItemsList().size() == 0) {
            if (result.related_streams.size() > 0) {
                Log.d(TAG,"Related streams "+result.related_streams.size());
                infoListAdapter.addInfoItemList(result.related_streams);
                showListFooter(hasMoreItems());
            } else {
                infoListAdapter.clearStreamItemList();
                showEmptyState();
            }
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    protected void setInitialData(int serviceId, String url, String name) {
        this.serviceId = serviceId;
        this.url = url;
        this.name = !TextUtils.isEmpty(name) ? name : "";
    }
}
