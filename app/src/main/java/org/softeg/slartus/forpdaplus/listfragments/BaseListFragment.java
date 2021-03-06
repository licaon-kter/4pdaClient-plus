package org.softeg.slartus.forpdaplus.listfragments;/*
 * Created by slinkin on 10.04.2014.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.softeg.slartus.forpdaapi.IListItem;
import org.softeg.slartus.forpdaplus.App;
import org.softeg.slartus.forpdaplus.R;
import org.softeg.slartus.forpdaplus.controls.ListViewLoadMoreFooter;
import org.softeg.slartus.forpdaplus.db.CacheDbHelper;
import org.softeg.slartus.forpdaplus.listfragments.adapters.ListAdapter;
import org.softeg.slartus.forpdaplus.prefs.Preferences;
import org.softeg.sqliteannotations.BaseDao;

import java.util.ArrayList;

public abstract class BaseListFragment extends BaseBrickFragment implements
        AdapterView.OnItemClickListener {
    protected ArrayList<IListItem> mData = new ArrayList<>();

    public static final String FIRST_VISIBLE_ROW_KEY = "FIRST_VISIBLE_ROW_KEY";
    public static final String TOP_KEY = "TOP_KEY";

    protected Handler mHandler = new Handler();
    private int m_FirstVisibleRow = 0;
    private int m_Top = 0;

    public BaseListFragment() {
        super();
    }

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

            m_FirstVisibleRow = savedInstanceState.getInt(FIRST_VISIBLE_ROW_KEY, m_FirstVisibleRow);
            m_Top = savedInstanceState.getInt(TOP_KEY, m_Top);
        }
    }

    protected View getListViewHeader() {
        return null;
    }

    private ListView mListView;
    private TextView mEmptyTextView;

    protected int getViewId() {
        return R.layout.list_fragment;
    }

    @Override
    public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, android.os.Bundle savedInstanceState) {
        view = inflater.inflate(getViewId(), container, false);
        assert view != null;
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        View header = getListViewHeader();
        if (header != null)
            mListView.addHeaderView(header);
        mEmptyTextView = (TextView) findViewById(android.R.id.empty);
        mListView.setEmptyView(mEmptyTextView);
        return view;
    }

    @Override
    public void onSaveInstanceState(android.os.Bundle outState) {
        if (args != null)
            outState.putAll(args);

        outState.putInt(FIRST_VISIBLE_ROW_KEY, m_FirstVisibleRow);
        outState.putInt(TOP_KEY, m_Top);
        super.onSaveInstanceState(outState);
    }

    public void setCount() {
        int count = mAdapter.getCount();
        mListViewLoadMoreFooter.setCount(count, count);
        mListViewLoadMoreFooter.setState(ListViewLoadMoreFooter.STATE_FULL_DOWNLOADED);
    }


    protected BaseAdapter mAdapter;
    protected ListViewLoadMoreFooter mListViewLoadMoreFooter;

    public Context getContext() {
        return getActivity();
    }

    protected SwipeRefreshLayout mSwipeRefreshLayout;

    protected void saveListViewScrollPosition() {
        if (getListView() == null)
            return;
        m_FirstVisibleRow = getListView().getFirstVisiblePosition();
        View v = getListView().getChildAt(0);
        m_Top = (v == null) ? 0 : v.getTop();
    }

    protected void restoreListViewScrollPosition() {
        getListView().setSelectionFromTop(m_FirstVisibleRow, m_Top);
    }

    protected ListView getListView() {
        return mListView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListViewLoadMoreFooter = new ListViewLoadMoreFooter(view.getContext(), getListView());
        mListViewLoadMoreFooter.setOnLoadMoreClickListener(view1 -> {
            mListViewLoadMoreFooter.setState(ListViewLoadMoreFooter.STATE_LOADING);
            loadData(false);
        });


        mSwipeRefreshLayout = createSwipeRefreshLayout(view);
    }

    protected SwipeRefreshLayout createSwipeRefreshLayout(View view) {
        final SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.ptr_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> loadData(true));
        swipeRefreshLayout.setColorSchemeResources(App.getInstance().getMainAccentColor());
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(App.getInstance().getSwipeRefreshBackground());
        return swipeRefreshLayout;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        registerForContextMenu(getListView());
        setListShown(false);
        mAdapter = createAdapter();


        setListAdapter(mAdapter);
    }

    private void setListAdapter(BaseAdapter mAdapter) {
        getListView().setAdapter(mAdapter);
    }

    protected void setListShown(boolean b) {
        //mListView.setVisibility(b?);
    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    private Runnable notifyAdapter = new Runnable() {
        @Override
        public void run() {
            getAdapter().notifyDataSetChanged();
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        if(getAdapter()!=null)
            mHandler.postDelayed(notifyAdapter, 300);

    }

    protected BaseAdapter createAdapter() {
        return new ListAdapter(getActivity(), mData, getPreferences().getBoolean("showSubMain", false));
    }

    protected void setLoading(final Boolean loading) {
        try {
            if (getActivity() == null) return;
            //mSwipeRefreshLayout.setRefreshing(loading);
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(loading));
            if (loading) {
                setEmptyText(App.getContext().getString(R.string.loading));
            } else {
                setEmptyText(App.getContext().getString(R.string.no_data));
            }
        } catch (Throwable ignore) {
            android.util.Log.e("TAG", ignore.toString());
        }
    }

    protected void setEmptyText(String s) {
        mEmptyTextView.setText(s);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!Preferences.Lists.getScrollByButtons())
            return false;

        int action = event.getAction();

        ListView scrollView = getListView();
        int visibleItemsCount = scrollView.getLastVisiblePosition() - scrollView.getFirstVisiblePosition();

        int keyCode = event.getKeyCode();
        if (Preferences.System.isScrollUpButton(keyCode)) {
            if (action == KeyEvent.ACTION_DOWN)
                scrollView.setSelection(Math.max(scrollView.getFirstVisiblePosition() - visibleItemsCount, 0));
            return true;// true надо обязательно возвращать даже если не ACTION_DOWN иначе звук нажатия
        }
        if (Preferences.System.isScrollDownButton(keyCode)) {
            if (action == KeyEvent.ACTION_DOWN)
                scrollView.setSelection(Math.min(scrollView.getLastVisiblePosition(), scrollView.getCount() - 1));
            return true;// true надо обязательно возвращать даже если не ACTION_DOWN иначе звук нажатия
        }

        return false;
    }

    protected <T> void updateItemCache(final T item, final Class<T> tClass, Boolean newThread) {
        if (newThread) {
            new Thread(() -> updateItemCache(item, tClass)).start();
        } else {
            updateItemCache(item, tClass);
        }
    }

    private <T> void updateItemCache(T item, Class<T> tClass) {
        try {
            CacheDbHelper cacheDbHelper = new CacheDbHelper(App.getContext());
            SQLiteDatabase db = null;
            try {
                db = cacheDbHelper.getWritableDatabase();
                BaseDao<T> baseDao = new BaseDao<>(App.getContext(), db, getListName(), tClass);

                baseDao.update(item, ((IListItem) item).getId().toString());

            } finally {
                if (db != null)
                    db.close();
            }
        } catch (Throwable ex) {
            // Log.e(getContext(), ex);
        }
    }
}
