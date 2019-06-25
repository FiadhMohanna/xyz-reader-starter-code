package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    private static final String ARG_ITEM_ID = "item_id";

    private String [] article;
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mDarkMutedColor = 0xFF333333;
    private int mDarkVibrantColor = 0xFF333333;
    private int mLightMutedColor = 0xFF333333;

    private TextView toolbarsubtitle;
    private AppBarLayout mAppBarLayout;
    private ImageView mPhotoView;
    private RecyclerView mRecyclerView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        setHasOptionsMenu(true);
    }

    private ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mAppBarLayout =  mRootView.findViewById(R.id.my_app_bar);
        mPhotoView =  mRootView.findViewById(R.id.photo);
        toolbarsubtitle =  mRootView.findViewById(R.id.subtitle);
        mRecyclerView = mRootView.findViewById(R.id.article_body);
        FloatingActionButton fab =  mRootView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Check out this article by " + mCursor.getString(ArticleLoader.Query.AUTHOR)
                        + " titled \"" + mCursor.getString(ArticleLoader.Query.TITLE)
                        + "\" available on " + getString(R.string.app_name);

                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(message)
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        bindViews();
        updateStatusBar();
        return mRootView;
    }

    private void updateStatusBar() {
        int color = Color.argb(255,
                (int) (Color.red(mDarkMutedColor) * 0.9),
                (int) (Color.green(mDarkMutedColor) * 0.9),
                (int) (Color.blue(mDarkMutedColor) * 0.9));
        toolbarsubtitle.setTextColor(mLightMutedColor);
        toolbarsubtitle.setBackgroundColor(mDarkVibrantColor);
        mAppBarLayout.setBackgroundColor(color);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }



        Toolbar mytoolbar = (Toolbar) mRootView.findViewById(R.id.my_toolbar);
        getActivityCast().setSupportActionBar(mytoolbar);
        mytoolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivityCast().onBackPressed();
            }
        });

        if (mCursor != null) {
            if (article == null){
                article = mCursor.getString(ArticleLoader.Query.BODY).split("(\r\n|\n|<br>|<br/>|<br />)");
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivityCast()));
                mRecyclerView.setAdapter(new Adapter(article));
            }
            mRootView.setVisibility(View.VISIBLE);
            mytoolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));

            Date publishedDate = parsePublishedDate();

            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                toolbarsubtitle.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                toolbarsubtitle.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = new Palette.Builder(bitmap).maximumColorCount(12).generate();
                                mDarkMutedColor = p.getDarkMutedColor(0xFF333333);
                                mDarkVibrantColor = p.getDarkVibrantColor(0xFF333333);
                                mLightMutedColor = p.getLightMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                updateStatusBar();
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        } else {
            mRootView.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
    private class Adapter extends RecyclerView.Adapter<ArticleDetailFragment.ViewHolder> {
        private String[] article;

        Adapter(String[] article) {
            this.article = article;
        }
        @Override
        public int getItemViewType(final int position) {
            return R.layout.article_line_item;
        }
        @Override
        public ArticleDetailFragment.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ArticleDetailFragment.ViewHolder holder, int position) {
            holder.articleLine.setText(Html.fromHtml(article[position]));
        }

        @Override
        public int getItemCount() {
            return article.length;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView articleLine;

        ViewHolder(View view) {
            super(view);
            articleLine =  view.findViewById(R.id.article_line);
        }
    }
}