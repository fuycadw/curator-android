package tw.wancw.curator;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

import java.util.Collection;

import tw.wancw.curator.api.CuratorApi;
import tw.wancw.curator.api.MeiZiCard;
import tw.wancw.curator.api.MeiZiCardsResponseHandler;
import tw.wancw.curator.widget.MeiZiCardAdapter;
import tw.wancw.widget.ListViewOnScrollListenerBroadcaster;

public class StreamFragment extends Fragment {

    private static final CuratorApi api = new CuratorApi(BuildConfig.CURATOR_API_TOKEN);

    private ImageLoader loader;

    protected MeiZiCardAdapter adapter;
    protected ListView cardsView;
    protected View loadingFooter;

    protected PaginatedStreamLoader streamLoader = new PaginatedStreamLoader();

    public StreamFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .build();

        ImageLoaderConfiguration imageLoaderConfig = new ImageLoaderConfiguration.Builder(activity)
            .defaultDisplayImageOptions(displayImageOptions)
            .build();

        loader = ImageLoader.getInstance();
        loader.init(imageLoaderConfig);

        adapter = new MeiZiCardAdapter(activity, loader);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stream, container, false);

        loadingFooter = inflater.inflate(R.layout.view_load_more, null);

        cardsView = (ListView) rootView.findViewById(R.id.cards);
        cardsView.setEmptyView(rootView.findViewById(android.R.id.empty));

        cardsView.addFooterView(loadingFooter);
        cardsView.setAdapter(adapter);
        cardsView.removeFooterView(loadingFooter);

        cardsView.setOnScrollListener(new ListViewOnScrollListenerBroadcaster(
            new PauseOnScrollListener(loader, true, true),
            new LoadMoreTrigger()
        ));

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        streamLoader.loadNextPage();
    }

    private class LoadMoreTrigger implements AbsListView.OnScrollListener {

        private boolean reachEnd = false;

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (visibleItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount) {
                streamLoader.loadNextPage();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            // Do nothing
        }
    }

    private class PaginatedStreamLoader implements MeiZiCardsResponseHandler {

        private int lastPage = 0;
        private boolean loading = false;
        private boolean noMoreData = false;

        public void loadNextPage() {
            if (loading || noMoreData) {
                return;
            }

            loading = true;

            cardsView.addFooterView(loadingFooter);

            lastPage = lastPage + 1;
            api.stream(lastPage, this);
        }

        @Override
        public void onSuccess(Collection<MeiZiCard> cards) {
            if (cards.size() > 0) {
                adapter.add(cards);
            } else {
                noMoreData = true;
            }

            cardsView.removeFooterView(loadingFooter);

            loading = false;
        }
    }
}