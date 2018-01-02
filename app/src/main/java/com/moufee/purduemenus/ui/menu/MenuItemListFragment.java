package com.moufee.purduemenus.ui.menu;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.moufee.purduemenus.R;
import com.moufee.purduemenus.menus.DailyMenuViewModel;
import com.moufee.purduemenus.menus.DiningCourtMenu;
import com.moufee.purduemenus.menus.FullDayMenu;
import com.moufee.purduemenus.menus.MenuItem;
import com.moufee.purduemenus.menus.MenuRecyclerViewAdapter;
import com.moufee.purduemenus.util.Resource;

import java.util.ArrayList;

/**
 * A Fragment that contains a list of menu items for one Meal at one Dining Court
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class MenuItemListFragment extends Fragment {

    private static final String ARG_DINING_COURT_INDEX = "dining-court-index";
    private static final String ARG_MEAL_INDEX = "meal-index";
    private static final String TAG = "MENU_ITEM_LIST_FRAGMENT";

    // TODO: restructure so that all data stays in ViewModel (data binding?)
    private int mDiningCourtIndex = 0;
    private int mMealIndex = 0;
    private OnListFragmentInteractionListener mListener;
    private RecyclerView mMenuItemRecyclerView;
    private TextView mNotServingTextView;
    private MenuRecyclerViewAdapter mDataBoundAdapter;
    private DailyMenuViewModel mViewModel;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MenuItemListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static MenuItemListFragment newInstance(int diningCourtIndex, int mealIndex) {
        MenuItemListFragment fragment = new MenuItemListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DINING_COURT_INDEX, diningCourtIndex);
        args.putInt(ARG_MEAL_INDEX, mealIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mDiningCourtIndex = getArguments().getInt(ARG_DINING_COURT_INDEX);
            mMealIndex = getArguments().getInt(ARG_MEAL_INDEX);
        }
        mViewModel = ViewModelProviders.of(getActivity()).get(DailyMenuViewModel.class);
        mDataBoundAdapter = new MenuRecyclerViewAdapter();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menuitem_list, container, false);


        // Set the adapter
        Context context = view.getContext();
        mMenuItemRecyclerView = (RecyclerView) view.findViewById(R.id.menu_item_recyclerview);
        mNotServingTextView = view.findViewById(R.id.not_serving_textview);
        mMenuItemRecyclerView.setAdapter(mDataBoundAdapter);

        mMenuItemRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        setListener();

        return view;
    }


    private void setListener() {
        mViewModel.getFullMenu().observe(this, new Observer<Resource<FullDayMenu>>() {
            @Override
            public void onChanged(@Nullable Resource<FullDayMenu> fullDayMenuResource) {
                try {
                    if (fullDayMenuResource != null && fullDayMenuResource.data != null && fullDayMenuResource.data.getMenu(mDiningCourtIndex).isServing(mMealIndex)) {
                        mDataBoundAdapter.setStations(fullDayMenuResource.data.getMenu(mDiningCourtIndex).getMeal(mMealIndex).getStations());
                        mMenuItemRecyclerView.setVisibility(View.VISIBLE);
                        mNotServingTextView.setVisibility(View.GONE);
                    } else {
                        mMenuItemRecyclerView.setVisibility(View.GONE);
                        mNotServingTextView.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    mDataBoundAdapter.setStations(new ArrayList<DiningCourtMenu.Station>());
                    Log.e(TAG, "onChanged: ", e);
                    mMenuItemRecyclerView.setVisibility(View.GONE);
                    mNotServingTextView.setVisibility(View.VISIBLE);
                }

            }
        });
        mViewModel.getSelectedMealIndex().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                if (integer == null)
                    return;
                mMealIndex = integer;
                Resource<FullDayMenu> fullDayMenuResource = mViewModel.getFullMenu().getValue();
                try {
                    if (fullDayMenuResource != null && fullDayMenuResource.data != null && fullDayMenuResource.data.getMenu(mDiningCourtIndex).isServing(mMealIndex)) {
//                        DiningCourtMenu.Meal meal = fullDayMenuResource.data.getMenu(mDiningCourtIndex).getMeal(mMealIndex);
                        mDataBoundAdapter.setStations(mViewModel.getFullMenu().getValue().data.getMenu(mDiningCourtIndex).getMeal(mMealIndex).getStations());
                        mMenuItemRecyclerView.setVisibility(View.VISIBLE);
                        mNotServingTextView.setVisibility(View.GONE);
                    } else {
                        mMenuItemRecyclerView.setVisibility(View.GONE);
                        mNotServingTextView.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    mDataBoundAdapter.setStations(new ArrayList<DiningCourtMenu.Station>());
                    Log.e(TAG, "onChanged: ", e);
                    mMenuItemRecyclerView.setVisibility(View.GONE);
                    mNotServingTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name

        void onListFragmentInteraction(MenuItem item);
    }
}
