package com.abdulrauf.filemanager.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.abdulrauf.filemanager.R;
import com.abdulrauf.filemanager.adapters.DisplayFragmentAdapter;
import com.abdulrauf.filemanager.dialogs.OnLongPressDialog;
import com.abdulrauf.filemanager.managers.EventManager;


import java.io.File;
import java.util.ArrayList;


/**
 * Created by abdul on 29/12/15.
 */
public class DisplayFragment extends Fragment  {


    private final String PREF_IS_CASE_SENSITIVE = "IS_CASE_SENSITIVE";
    private final String PREF_SHOW_HIDDEN_FILES = "SHOW_HIDDEN_FILES";
    private final String PREF_SORT_ORDER = "SORT_ORDER";
    private final String PREF_SORT_BY = "SORT_BY";


    private RecyclerView recyclerView;
    private File path;
    private ArrayList<File> filesAndFolders;
    private Toolbar toolbar;
    private DisplayFragmentAdapter adapter;
    private ActionMode actionMode;
    private DialogFragment longPressDialog;
    private SharedPreferences prefs;
    private boolean clickAllowed;


      @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String temp;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)

             temp = "/";

        else temp = Environment.getExternalStorageDirectory().toString();

        path = new File(temp);

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display,container,false);

        setRetainInstance(true);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(),2);
        gridLayoutManager.setOrientation(GridLayoutManager.VERTICAL);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle(path.getName());

        filesAndFolders = new ArrayList<>();

        adapter = new DisplayFragmentAdapter(filesAndFolders,onItemClickListenerCallback,getActivity());
        EventManager.getInstance().init(getActivity(), this, filesAndFolders, adapter);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        setPrefs();
        EventManager.getInstance().getFileManager().initialisePathStackWithAbsolutePath(true,path);

        recyclerView.setLayoutManager(gridLayoutManager);
        EventManager.getInstance().open(path);
        recyclerView.setAdapter(adapter);

        clickAllowed = true;
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        setPrefs();
        EventManager.getInstance().refreshCurrentDirectory();
    }

    private void setPrefs() {

        EventManager
                .getInstance()
                .getFileManager()
                .setShowHiddenFiles(
                        prefs.getBoolean(PREF_IS_CASE_SENSITIVE, false)
                );

        EventManager
                .getInstance()
                .getFileManager()
                .setSortingStyle(
                    prefs.getString(PREF_SORT_ORDER, EventManager.SORT_ORDER_ASC),
                    prefs.getString(PREF_SORT_BY, EventManager.SORT_BY_NAME),
                    prefs.getBoolean(PREF_SHOW_HIDDEN_FILES, false)
                );

    }

    private DisplayFragmentAdapter.OnItemClickListener onItemClickListenerCallback = new DisplayFragmentAdapter.OnItemClickListener() {

        @Override
        public void onItemClick(View view, int position) {
            File singleItem = filesAndFolders.get(position);

            if(clickAllowed)
                EventManager.getInstance().open(singleItem);

        }

        @Override
        public void onItemLongClick(View view, int position) {

            if(clickAllowed) {
                longPressDialog = OnLongPressDialog.newInstance(onLongPressListenerCallback,position);
                longPressDialog.show(getFragmentManager(), "onLongPressDialog");
            }
        }

        @Override
        public void onIconClick(View view, int position) {

            clickAllowed = false;

            if (actionMode != null) {

                adapter.toggleSelection(position);
                actionMode.setTitle(adapter.getSelectedItemsCount() + "  " + getString(R.string.info_items_selected));

                if (adapter.getSelectedItemsCount() <= 0)
                    actionMode.finish();

                return;
            }

            actionMode = getActivity().startActionMode(actionModeCallback);
            adapter.toggleSelection(position);
            actionMode.setTitle(adapter.getSelectedItemsCount() + "  " + getString(R.string.info_items_selected));
        }
    };



    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater menuInflater = mode.getMenuInflater();
            menuInflater.inflate(R.menu.menu_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, final MenuItem item) {

            switch (item.getItemId()) {

                case R.id.shareButton1 :
                    Toast.makeText(getActivity(), "Share Button Clicked", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    return true;

                case R.id.deleteButton1 :
                    EventManager.getInstance().delete(adapter.getSelectedItems());
                    mode.finish();
                    return true;

                case R.id.moveButton1 :
                case R.id.copyButton1 :

                    selectTargetAndPerformOperation(adapter.getSelectedItems(), item.getItemId());
                    mode.finish();
                    return true;

                default:
                    return false;
            }
        }



        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            adapter.clearSelection();
            clickAllowed = true;
        }

    };


    private OnLongPressDialog.OnLongPressListener  onLongPressListenerCallback = new  OnLongPressDialog.OnLongPressListener() {

        @Override
        public void onOpenButtonClicked(int position) {

            EventManager
                    .getInstance()
                    .open(filesAndFolders.get(position));
            longPressDialog.dismiss();
        }

        @Override
        public void onShareButtonClicked(int position) {

            ArrayList<File> files = new ArrayList<File>();
            files.add(filesAndFolders.get(position));
            EventManager.getInstance().share(files);
            longPressDialog.dismiss();
        }

        @Override
        public void onDeleteButtonClicked(int position) {
            ArrayList<File> files = new ArrayList<>();
            files.add(filesAndFolders.get(position));
            EventManager.getInstance().delete(files);
            longPressDialog.dismiss();
        }

        @Override
        public void onRenameButtonClicked(int position) {

            promptUserForRenameInput(filesAndFolders.get(position));
            longPressDialog.dismiss();
        }

        @Override
        public void onCopyButtonClicked(int position) {

            ArrayList<File> list = new ArrayList<>();
            list.add(filesAndFolders.get(position));
            selectTargetAndPerformOperation(list, R.id.copyButton1);
            longPressDialog.dismiss();
        }

        @Override
        public void onMoveButtonClicked(int position) {

            ArrayList<File> list = new ArrayList<>();
            list.add(filesAndFolders.get(position));
            selectTargetAndPerformOperation(list, R.id.moveButton1);
            longPressDialog.dismiss();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }

        @Override
        public int describeContents() {
            return 0;
        }
    };


    private void selectTargetAndPerformOperation(final ArrayList<File> list,final int id) {

        Toast.makeText(getActivity(), getString(R.string.prompt_select_destination), Toast.LENGTH_SHORT).show();
        toolbar.inflateMenu(R.menu.menu_copy_move);

        toolbar.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                adapter.clearSelection();
                toolbar
                        .getMenu()
                        .clear();

                toolbar.inflateMenu(R.menu.menu_main);
            }
        });

        toolbar.findViewById(R.id.selectButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File target = EventManager.getInstance()
                        .getFileManager()
                        .getCurrentDirectory();

                switch (id) {

                    case R.id.copyButton1:
                        EventManager
                                .getInstance()
                                .copy(list, target);
                        break;

                    case R.id.moveButton1:
                        EventManager
                                .getInstance()
                                .move(list, target);
                        break;
                }

                adapter.clearSelection();
                toolbar
                        .getMenu()
                        .clear();

                toolbar.inflateMenu(R.menu.menu_main);

            }
        });

    }

    private void promptUserForRenameInput(final File file) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText editText = new EditText(getActivity());
        editText.setText(file.getName());

        builder.setMessage(getString(R.string.prompt_rename_newName))
                .setView(editText)
                .setPositiveButton(getString(R.string.rename), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(EventManager.getInstance().getFileManager().renameFileTo(file,editText.getText().toString())) {
                            Toast.makeText(getActivity(), getString(R.string.success_rename), Toast.LENGTH_SHORT).show();
                            EventManager.getInstance().populateList(EventManager.getInstance().getFileManager().getCurrentDirectory());
                        }
                        else Toast.makeText(getActivity(),getString(R.string.error_rename),Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton( getString(R.string.cancel), null)
                .create()
                .show();
    }


    public Toolbar getToolbar() {
        return toolbar;
    }


}
