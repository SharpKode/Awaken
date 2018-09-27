package io.awaken.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.fabtransitionactivity.SheetLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.awaken.R;
import io.awaken.activities.NewConnectionActivity;
import io.awaken.adapters.ConnectionsAdapter;
import io.awaken.components.Connection;
import io.awaken.database.ConnectionDatabaseHelper;
import io.awaken.utilities.AnimatedRecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @author Tyler Wong
 */
public class ConnectionsFragment extends Fragment implements SheetLayout.OnFabAnimationEndListener, ConnectionRefresher {

    private SheetLayout mSheetLayout;
    private FloatingActionButton mFab;
    private AnimatedRecyclerView mConnectionsList;
    private SwipeRefreshLayout mRefreshLayout;
    LinearLayoutManager mLayoutManager;
    private LinearLayout mEmptyView;
    private List<Connection> mConnections;

    private ConnectionsAdapter mConnectionsAdapter;

    private ConnectionDatabaseHelper mDatabaseHelper;

    private final static int REQUEST_CODE = 1;
    private final static int DURATION = 1000;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.connections_fragment, container, false);

        mSheetLayout = view.findViewById(R.id.bottom_sheet);
        mConnectionsList = view.findViewById(R.id.connection_list);
        mRefreshLayout = view.findViewById(R.id.refresh_layout);
        mFab = view.findViewById(R.id.fab);
        mEmptyView = view.findViewById(R.id.empty_layout);

        mDatabaseHelper = new ConnectionDatabaseHelper(getContext());

        mRefreshLayout.setOnRefreshListener(() -> new Handler().postDelayed(() -> {
            refreshConnections();
            mRefreshLayout.setRefreshing(false);
        }, DURATION));

        mFab.setOnClickListener(itemView -> onFabClick());

        mSheetLayout.setFab(mFab);
        mSheetLayout.setFabAnimationEndListener(this);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.connections);
        }

        mConnections = mDatabaseHelper.getAllConnections();

        mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setOrientation(RecyclerView.VERTICAL);
        mConnectionsList.setLayoutManager(mLayoutManager);
        mConnectionsAdapter = new ConnectionsAdapter(mConnectionsList, mConnections, this);
        mConnectionsList.setAdapter(mConnectionsAdapter);

        return view;
    }

    private void onFabClick() {
        mSheetLayout.expandFab();
    }

    @SuppressWarnings("CheckResult")
    @Override
    public void refreshConnections() {
        for (int index = 0; index < mConnections.size(); index++) {
            final Connection connection = mConnections.get(index);
            final int connectionId = connection.getId();
            connection.isRunning()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            status -> mDatabaseHelper.updateStatus(connectionId, String.valueOf(status)),
                            throwable -> Log.e("ERROR", "Could not update status")
                    );
        }
        mConnections = mDatabaseHelper.getAllConnections();
        mConnectionsAdapter.setConnections(mConnections);
    }

    @Override
    public void onFabAnimationEnd() {
        Intent intent = new Intent(getContext(), NewConnectionActivity.class);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            mSheetLayout.contractFab();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshConnections();
    }
}