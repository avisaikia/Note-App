package com.developerdepository.noted;
import android.annotation.SuppressLint;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;

import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.developerdepository.noted.adapters.NotesAdapter;
import com.developerdepository.noted.database.NotesDatabase;
import com.developerdepository.noted.entities.Note;
import com.developerdepository.noted.listeners.NotesListener;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import com.tapadoo.alerter.Alerter;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class MainActivity extends AppCompatActivity implements NotesListener, NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout , settings;
    private NavigationView navigationView;
    private ConstraintLayout contentView;
    private ImageView imageEmpty;
    private ImageButton navigationMenu, ivViewType;
    private TextView textEmpty , backup,restore ;
    private EditText inputSearch;
    private RecyclerView notesRecyclerView;
    private BottomAppBar bottomAppBar;
    private FloatingActionButton addNoteFloatingBtn;

    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    public static final int REQUEST_CODE_TAKE_PHOTO = 4;
    public static final int REQUEST_CODE_SELECT_IMAGE = 5;
    public static final int REQUEST_CODE_VOICE_NOTE = 6;

    private int noteClickedPosition = -1;
    private androidx.appcompat.view.ActionMode actionMode;
    private AlertDialog dialogAddURL;
    private AlertDialog dialogAddImage;

    private int currentViewType = 1;
    private static final float END_SCALE = 0.8f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setNavigationBarColor(ContextCompat.getColor(MainActivity.this, R.color.colorQuickActionsBackground));
        initViews();
        setNavigationMenu();
        setActionOnViews();
        getNotes(REQUEST_CODE_SHOW_NOTES, false);
    }

    //initialize or setting up views in a user interface (UI) of an application.
    private void initViews() {
        drawerLayout = findViewById(R.id.main_drawer_layout);
        navigationView = findViewById(R.id.main_navigation_menu);
        contentView = findViewById(R.id.content_view);
        navigationMenu = findViewById(R.id.main_navigation);
        imageEmpty = findViewById(R.id.image_empty);
        textEmpty = findViewById(R.id.text_empty);
        inputSearch = findViewById(R.id.input_search);
        notesRecyclerView = findViewById(R.id.notes_recycler_view);
        bottomAppBar = findViewById(R.id.main_bottom_app_bar);
        addNoteFloatingBtn = findViewById(R.id.floating_action_add_notes_btn);
        ivViewType = findViewById(R.id.ivViewType);
    }

    // set actions or event handlers on views in a user interface (UI) of an application.
   // behavior of  views in response to user interaction , such as clicking a button or entering text into a text field
    @SuppressLint("NonConstantResourceId")
    private void setActionOnViews()
    {
        KeyboardVisibilityEvent.setEventListener(MainActivity.this, isOpen -> {
            if (!isOpen) {
                inputSearch.clearFocus();
            }
        });

        //Searching Notes
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }
            @Override
            public void afterTextChanged(Editable s) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });

        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);
        setViewType();

        //Bottom App Bar Features
        bottomAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_add:
                    UIUtil.hideKeyboard(MainActivity.this);
                    Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    CustomIntent.customType(MainActivity.this, "left-to-right");
                    inputSearch.setText(null);
                    break;
                case R.id.menu_image:
                    UIUtil.hideKeyboard(MainActivity.this);
                    showAddImageDialog();
                    break;
             case R.id.menu_voice:
                   UIUtil.hideKeyboard(MainActivity.this);
                 voiceNote();
                  break;
                case R.id.menu_web_link:
                    showAddURLDialog();
                    break;
            }
            return false;
        });

        //Floating Add Note Button
        addNoteFloatingBtn.setOnClickListener(v -> {
            UIUtil.hideKeyboard(MainActivity.this);
            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
            CustomIntent.customType(MainActivity.this, "left-to-right");
            inputSearch.setText(null);
        });
    }


    // View Type (Gird / List)
    private void setViewType() {
        ImageView imageView = findViewById(R.id.ivViewType);
        if (currentViewType == 1)
        {
            notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            imageView.setImageResource(R.drawable.gird);
            currentViewType = 2;
        }
        else
        {
            notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            imageView.setImageResource(R.drawable.listmenu);
            currentViewType = 1;
        }
        ivViewType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setViewType();
            }
        });
    }

    //Side Navigation Panel
    private void setNavigationMenu() {
        navigationView.bringToFront();
        navigationView.setNavigationItemSelectedListener(MainActivity.this);

        navigationMenu.setOnClickListener(v -> {
            UIUtil.hideKeyboard(MainActivity.this);
            if (drawerLayout.isDrawerVisible(GravityCompat.END))
                drawerLayout.closeDrawer(GravityCompat.END);
            else drawerLayout.openDrawer(GravityCompat.END);
        });

        animateNavigationDrawer();
    }

    //Side Navigation Panel Features
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_note:
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                CustomIntent.customType(MainActivity.this, "left-to-right");
                inputSearch.setText(null);
                break;
            case R.id.menu_add_image:
                showAddImageDialog();
                break;
            case R.id.menu_add_voice:
             voiceNote();
              break;
            case R.id.menu_add_url:
                showAddURLDialog();
                break;
            case R.id.menu_privacy_policy:
                String privacyPolicyUrl = "https://abhinavsaikia618.wixsite.com/my-site";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
                startActivity(browserIntent);
                inputSearch.setText(null);
                drawerLayout.closeDrawer(GravityCompat.END);

                break;
        }
        return false;
    }






    //Animation of Side Navigation Panel
    private void animateNavigationDrawer() {
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                final float diffScaledOffset = slideOffset * (1 - END_SCALE);
                final float offsetScale = 1 - diffScaledOffset;
                contentView.setScaleX(offsetScale);
                contentView.setScaleY(offsetScale);

                final float xOffset = drawerView.getWidth() * slideOffset;
                final float xOffsetDiff = contentView.getWidth() * diffScaledOffset / 2;
                final float xTranslation = xOffsetDiff - xOffset;
                contentView.setTranslationX(xTranslation);
            }
        });
    }

    // adding image by quickaction
    private void showAddImageDialog() {
        if (dialogAddImage == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_image,
                    (ViewGroup) findViewById(R.id.layout_add_image_container)
            );
            builder.setView(view);

            dialogAddImage = builder.create();
            if (dialogAddImage.getWindow() != null) {
                dialogAddImage.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.layout_take_photo).setOnClickListener(v -> {
                takePhoto();
                dialogAddImage.dismiss();
            });
            view.findViewById(R.id.layout_add_image).setOnClickListener(v -> {
                selectImage();
                dialogAddImage.dismiss();
            });
        }
        dialogAddImage.show();
    }

    // Taking photo by accessing the camera
    private void takePhoto() {
        ImagePicker.Companion.with(MainActivity.this)
                .cameraOnly()
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start(REQUEST_CODE_TAKE_PHOTO);
        // The constant REQUEST_CODE_TAKE_PHOTO is used as the request code,
        // to identify the result of the image capture operation in the onActivityResult() method.
     }

     // Selects photo from gallery/device storage
    private void selectImage() {
        ImagePicker.Companion.with(MainActivity.this)
                .galleryOnly()
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start(REQUEST_CODE_SELECT_IMAGE);
    }

    //retrieving the file path from a given content URI(Uniform Resource identifier)
    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    //Adding note using Voice assistant
   private void voiceNote()
   {
       Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
       intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
       intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
       intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something to add note!");
       startActivityForResult(intent, REQUEST_CODE_VOICE_NOTE);
  }

  //Adding URL by quick action
    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    (ViewGroup) findViewById(R.id.layout_add_url_container)
            );
            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.input_url);
            inputURL.requestFocus();

            view.findViewById(R.id.dialog_add_btn).setOnClickListener(v -> {
                if (inputURL.getText().toString().trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString().trim()).matches()) {
                    Toast.makeText(MainActivity.this, "Enter Valid URL", Toast.LENGTH_SHORT).show();
                } else {
                    dialogAddURL.dismiss();
                    UIUtil.hideKeyboard(view.getContext(), inputURL);
                    Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                    intent.putExtra("isFromQuickActions", true);
                    intent.putExtra("quickActionType", "URL");
                    intent.putExtra("URL", inputURL.getText().toString().trim());
                    startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                    CustomIntent.customType(MainActivity.this, "left-to-right");
                    inputSearch.setText(null);
                }
            });

            view.findViewById(R.id.dialog_cancel_btn).setOnClickListener(v -> {
                UIUtil.hideKeyboard(view.getContext(), inputURL);
                dialogAddURL.dismiss();
            });
        }
        dialogAddURL.setCancelable(false);
        dialogAddURL.show();
    }

    //Retrieving Notes from Database
    private void getNotes(final int requestCode, final boolean isNoteDeleted) {
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase
                        .getDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }




            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                    if (drawerLayout.isDrawerVisible(GravityCompat.END))
                        drawerLayout.closeDrawer(GravityCompat.END);
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                    if (drawerLayout.isDrawerVisible(GravityCompat.END))
                        drawerLayout.closeDrawer(GravityCompat.END);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.remove(noteClickedPosition);
                    if (isNoteDeleted) {
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    } else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                    if (drawerLayout.isDrawerVisible(GravityCompat.END))
                        drawerLayout.closeDrawer(GravityCompat.END);
                }
                if (noteList.size() != 0) {
                    imageEmpty.setVisibility(View.GONE);
                    textEmpty.setVisibility(View.GONE);
                } else {
                    imageEmpty.setVisibility(View.VISIBLE);
                    textEmpty.setVisibility(View.VISIBLE);
                }
            }
        }
        new GetNotesTask().execute();
    }

    //Action on Clicking a note on recycler view
    @Override
    public void onNoteClicked(View view, Note note, int position)
    {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        CustomIntent.customType(MainActivity.this, "left-to-right");
        inputSearch.setText(null);
    }

    //Long-Pressed on a note---
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onNoteLongClicked(View view, Note note, int position)
    {
        noteClickedPosition = position;
        view.setForeground(getDrawable(R.drawable.foreground_selected_note));
        if (actionMode != null)
        {
            return;
        }

        actionMode = startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu)
            {
                mode.getMenuInflater().inflate(R.menu.note_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            //to edit a note , share and delete
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.note_menu_edit:
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isViewOrUpdate", true);
                        intent.putExtra("note", note);
                        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
                        CustomIntent.customType(MainActivity.this, "left-to-right");
                        inputSearch.setText(null);
                        mode.finish();
                        return true;
                    case R.id.note_menu_share:
                        if (note.getImagePath() == null) {
                            String content = note.getTitle() + "\n\n" + note.getSubtitle() + "\n\n" + note.getNoteText();
                            Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitle());
                            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);
                            startActivity(Intent.createChooser(shareIntent, "Share via"));
                        } else {
                            String textContent = note.getTitle() + "\n\n" + note.getSubtitle() + "\n\n" + note.getNoteText();
                            Bitmap bitmap = BitmapFactory.decodeFile(note.getImagePath());
                            String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "title", null);
                            Uri bitmapUri = Uri.parse(bitmapPath);
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("image/png");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, textContent);
                            startActivity(Intent.createChooser(shareIntent, "Share"));
                        }
                        mode.finish();
                        return true;
                    case R.id.note_menu_delete:
                        showDeleteNoteDialog(note);
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                view.setForeground(null);
            }
        });
    }

    //Deleting a note
    private void showDeleteNoteDialog(Note note) {
        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                .setTitle("Are you sure?")
                .setMessage("Are you sure you want to delete this note?")
                .setAnimation(R.raw.lottie_delete)
                .setCancelable(false)
                .setPositiveButton("Delete", R.drawable.ic_material_dialog_delete, (dialogInterface, which) -> {
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void, Void, Void> {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao()
                                    .deleteNote(note);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            noteList.remove(noteClickedPosition);
                            notesAdapter.notifyItemRemoved(noteClickedPosition);

                            if (noteList.size() != 0) {
                                imageEmpty.setVisibility(View.GONE);
                                textEmpty.setVisibility(View.GONE);
                            } else {
                                imageEmpty.setVisibility(View.VISIBLE);
                                textEmpty.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    new DeleteNoteTask().execute();
                    dialogInterface.dismiss();
                })
                .setNegativeButton("Cancel", R.drawable.ic_material_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss())
                .build();
        materialDialog.show();
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK) {
            getNotes(REQUEST_CODE_ADD_NOTE, false);
        } else if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK) {
            if (data != null) {
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
            }
        } else if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (data != null) {
                Uri takePhotoUri = data.getData();
                if (takePhotoUri != null) {
                    try {
                        String selectedImagePath = getPathFromUri(takePhotoUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                        CustomIntent.customType(MainActivity.this, "left-to-right");
                        inputSearch.setText(null);
                    }
                    catch (Exception exception)
                    {
                        Alerter.create(MainActivity.this)
                                .setText("Some ERROR occurred!")
                                .setTextAppearance(R.style.ErrorAlert)
                                .setBackgroundColorRes(R.color.errorColor)
                                .setIcon(R.drawable.ic_error)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getResources().getColor(android.R.color.white))
                                .show();
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            if (data != null)
            {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null)
                {
                    try {
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
                        CustomIntent.customType(MainActivity.this, "left-to-right");
                        inputSearch.setText(null);
                    } catch (Exception exception) {
                        Alerter.create(MainActivity.this)
                                .setText("Some ERROR occurred!")
                                .setTextAppearance(R.style.ErrorAlert)
                                .setBackgroundColorRes(R.color.errorColor)
                                .setIcon(R.drawable.ic_error)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getResources().getColor(android.R.color.white))
                                .show();
                    }
                }
            }
      }
        else if (requestCode == REQUEST_CODE_VOICE_NOTE && resultCode == RESULT_OK) {
           if (data != null)
           {
               ArrayList<String> voiceResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
              Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
               intent.putExtra("isFromQuickActions", true);
               intent.putExtra("quickActionType", "voiceNote");
               intent.putExtra("inputText", voiceResult.get(0));
                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
              CustomIntent.customType(MainActivity.this, "left-to-right");
               inputSearch.setText(null);
            }
        }
    }

    //Navigation Drawer closing if open
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerVisible(GravityCompat.END))
           drawerLayout.closeDrawer(GravityCompat.END);
      else finishAffinity();
   }
}