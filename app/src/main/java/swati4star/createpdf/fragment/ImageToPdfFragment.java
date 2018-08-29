package swati4star.createpdf.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dd.morphingbutton.MorphingButton;
import com.theartofdev.edmodo.cropper.CropImage;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import swati4star.createpdf.R;
import swati4star.createpdf.activity.ImageEditor;
import swati4star.createpdf.activity.PreviewActivity;
import swati4star.createpdf.activity.RearrangeImages;
import swati4star.createpdf.adapter.EnhancementOptionsAdapter;
import swati4star.createpdf.interfaces.OnItemClickListner;
import swati4star.createpdf.interfaces.OnPDFCreatedInterface;
import swati4star.createpdf.model.EnhancementOptionsEntity;
import swati4star.createpdf.model.ImageToPDFOptions;
import swati4star.createpdf.util.Constants;
import swati4star.createpdf.util.CreatePdf;
import swati4star.createpdf.util.FileUtils;
import swati4star.createpdf.util.MorphButtonUtility;
import swati4star.createpdf.util.PageSizeUtils;
import swati4star.createpdf.util.StringUtils;

import static swati4star.createpdf.util.Constants.DEFAULT_COMPRESSION;
import static swati4star.createpdf.util.Constants.DEFAULT_IMAGE_BORDER_TEXT;
import static swati4star.createpdf.util.Constants.IMAGE_EDITOR_KEY;
import static swati4star.createpdf.util.Constants.PREVIEW_IMAGES;
import static swati4star.createpdf.util.Constants.RESULT;
import static swati4star.createpdf.util.ImageEnhancementOptionsUtils.getEnhancementOptions;
import static swati4star.createpdf.util.StringUtils.showSnackbar;

/**
 * ImageToPdfFragment fragment to start with creating PDF
 */
public class ImageToPdfFragment extends Fragment implements OnItemClickListner,
        OnPDFCreatedInterface {

    private static final int INTENT_REQUEST_GET_IMAGES = 13;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT = 1;
    private static final int INTENT_REQUEST_APPLY_FILTER = 10;
    private static final int INTENT_REQUEST_PREVIEW_IMAGE = 11;
    private static final int INTENT_REQUEST_REARRANGE_IMAGE = 12;

    @BindView(R.id.addImages)
    MorphingButton addImages;
    @BindView(R.id.pdfCreate)
    MorphingButton mCreatePdf;
    @BindView(R.id.pdfOpen)
    MorphingButton mOpenPdf;
    @BindView(R.id.enhancement_options_recycle_view)
    RecyclerView mEnhancementOptionsRecycleView;
    @BindView(R.id.tvNoOfImages)
    TextView mNoOfImages;

    private ArrayList<EnhancementOptionsEntity> mEnhancementOptionsEntityArrayList = new ArrayList<>();
    private MorphButtonUtility mMorphButtonUtility;
    private Activity mActivity;
    private ArrayList<String> mImagesUri = new ArrayList<>();
    private String mPath;
    private boolean mOpenSelectImages = false;
    private SharedPreferences mSharedPreferences;
    private EnhancementOptionsAdapter mEnhancementOptionsAdapter;
    private FileUtils mFileUtils;
    private int mButtonClicked = 0;
    private static int mImageCounter = 0;
    private ImageToPDFOptions mPdfOptions;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, root);

        // Initialize variables
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        mFileUtils = new FileUtils(mActivity);
        mPdfOptions = new ImageToPDFOptions();
        PageSizeUtils.mPageSize = mSharedPreferences.getString(Constants.DEFAULT_PAGE_SIZE_TEXT ,
                Constants.DEFAULT_PAGE_SIZE);
        mMorphButtonUtility.morphToGrey(mCreatePdf, mMorphButtonUtility.integer());
        mCreatePdf.setEnabled(false);
        mOpenPdf.setVisibility(View.GONE);

        // Get runtime permissions if build version >= Android M
        getRuntimePermissions(false);

        showEnhancementOptions();

        // Check for the images received
        Bundle bundle = getArguments();
        if (bundle != null) {
            ArrayList<Parcelable> uris = bundle.getParcelableArrayList(getString(R.string.bundleKey));
            for (Parcelable p : uris) {
                Uri uri = (Uri) p;
                if (mFileUtils.getUriRealPath(uri) == null) {
                    showSnackbar(mActivity, R.string.whatsappToast);
                } else {
                    mImagesUri.add(mFileUtils.getUriRealPath(uri));
                    if (mImagesUri.size() > 0) {
                        mNoOfImages.setText(String.format(mActivity.getResources()
                                .getString(R.string.images_selected), mImagesUri.size()));
                        mNoOfImages.setVisibility(View.VISIBLE);
                    } else {
                        mNoOfImages.setVisibility(View.GONE);
                    }
                    showSnackbar(mActivity, R.string.successToast);
                }
            }
        }

        mPdfOptions.setBorderWidth(mSharedPreferences.getInt(DEFAULT_IMAGE_BORDER_TEXT, 0));
        showBorderWidth();

        return root;
    }

    private void showEnhancementOptions() {
        GridLayoutManager mGridLayoutManager = new GridLayoutManager(getActivity(), 2);
        mEnhancementOptionsRecycleView.setLayoutManager(mGridLayoutManager);
        mEnhancementOptionsEntityArrayList = getEnhancementOptions(mActivity);
        mEnhancementOptionsAdapter = new EnhancementOptionsAdapter(this, mEnhancementOptionsEntityArrayList);
        mEnhancementOptionsRecycleView.setAdapter(mEnhancementOptionsAdapter);
    }

    /**
     * Adding Images to PDF
     */
    @OnClick(R.id.addImages)
    void startAddingImages() {
        if (mButtonClicked == 0) {
            if (getRuntimePermissions(true))
                selectImages();
            mButtonClicked = 1;
        }
    }

    private void filterImages() {
        try {
            Intent intent = new Intent(getContext(), ImageEditor.class);
            intent.putStringArrayListExtra(IMAGE_EDITOR_KEY, mImagesUri);
            startActivityForResult(intent, INTENT_REQUEST_APPLY_FILTER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create Pdf of selected images
    @OnClick({R.id.pdfCreate})
    void createPdf() {
        mPdfOptions.setImagesUri(mImagesUri);
        mPdfOptions.setPageSize(PageSizeUtils.mPageSize);

        new MaterialDialog.Builder(mActivity)
                .title(R.string.creating_pdf)
                .content(R.string.enter_file_name)
                .input(getString(R.string.example), null, (dialog, input) -> {
                    if (StringUtils.isEmpty(input)) {
                        showSnackbar(mActivity, R.string.snackbar_name_not_blank);
                    } else {
                        final String filename = input.toString();
                        FileUtils utils = new FileUtils(mActivity);
                        if (!utils.isFileExist(filename + getString(R.string.pdf_ext))) {
                            mPdfOptions.setOutFileName(filename);
                            new CreatePdf(mActivity, mPdfOptions,
                                    ImageToPdfFragment.this).execute();
                        } else {
                            new MaterialDialog.Builder(mActivity)
                                    .title(R.string.warning)
                                    .content(R.string.overwrite_message)
                                    .positiveText(android.R.string.ok)
                                    .negativeText(android.R.string.cancel)
                                    .onPositive((dialog12, which) -> {
                                        mPdfOptions.setOutFileName(filename);
                                        new CreatePdf(mActivity, mPdfOptions,
                                            ImageToPdfFragment.this).execute();
                                    })
                                    .onNegative((dialog1, which) -> createPdf())
                                    .show();
                        }
                    }
                })
                .show();
    }

    @OnClick(R.id.pdfOpen)
    void openPdf() {
        mFileUtils.openFile(mPath);
    }

    /**
     * Called after user is asked to grant permissions
     *
     * @param requestCode  REQUEST Code for opening permissions
     * @param permissions  permissions asked to user
     * @param grantResults bool array indicating if permission is granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (grantResults.length < 1)
            return;

        switch (requestCode) {
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mOpenSelectImages)
                        selectImages();
                    showSnackbar(mActivity, R.string.snackbar_permissions_given);
                } else
                    showSnackbar(mActivity, R.string.snackbar_insufficient_permissions);
            }
        }
    }

    /**
     * Called after Matisse Activity is called
     *
     * @param requestCode REQUEST Code for opening Matisse Activity
     * @param resultCode  result code of the process
     * @param data        Data of the image selected
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mButtonClicked = 0;
        if (resultCode != Activity.RESULT_OK)
            return;

        switch (requestCode) {
            case INTENT_REQUEST_GET_IMAGES:
                mImagesUri.clear();
                List<Uri> imageUris = Matisse.obtainResult(data);
                for (Uri uri : imageUris)
                    mImagesUri.add(mFileUtils.getUriRealPath(uri));
                if (imageUris.size() > 0) {
                    mNoOfImages.setText(String.format(mActivity.getResources()
                            .getString(R.string.images_selected), imageUris.size()));
                    mNoOfImages.setVisibility(View.VISIBLE);
                    showSnackbar(mActivity, R.string.snackbar_images_added);
                    mCreatePdf.setEnabled(true);
                } else {
                    mNoOfImages.setVisibility(View.GONE);
                }
                mMorphButtonUtility.morphToSquare(mCreatePdf, mMorphButtonUtility.integer());
                mOpenPdf.setVisibility(View.GONE);
                break;

            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                Uri resultUri = result.getUri();
                if (mImagesUri.size() > mImageCounter)
                    mImagesUri.set(mImageCounter, resultUri.getPath());
                showSnackbar(mActivity, R.string.snackbar_imagecropped);
                mImageCounter++;
                cropNextImage();
                break;

            case INTENT_REQUEST_APPLY_FILTER:
                try {
                    mImagesUri.clear();
                    ArrayList<String> mFilterUris = data.getStringArrayListExtra(RESULT);
                    int size = mFilterUris.size() - 1;
                    for (int k = 0; k <= size; k++) {
                        mImagesUri.add(mFilterUris.get(k));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            case INTENT_REQUEST_PREVIEW_IMAGE:
                try {
                    mImagesUri = data.getStringArrayListExtra(RESULT);
                    if (mImagesUri.size() > 0) {
                        mNoOfImages.setText(String.format(mActivity.getResources()
                                        .getString(R.string.images_selected), mImagesUri.size()));
                    } else {
                        mNoOfImages.setVisibility(View.GONE);
                        mMorphButtonUtility.morphToGrey(mCreatePdf, mMorphButtonUtility.integer());
                        mCreatePdf.setEnabled(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case INTENT_REQUEST_REARRANGE_IMAGE:
                try {
                    mImagesUri = data.getStringArrayListExtra(Constants.RESULT);
                    showSnackbar(mActivity, R.string.images_rearranged);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    @Override
    public void onItemClick(int position) {

        if (mImagesUri.size() == 0) {
            showSnackbar(mActivity, R.string.snackbar_no_images);
            return;
        }
        switch (position) {
            case 0:
                passwordProtectPDF();
                break;
            case 1:
                cropNextImage();
                break;
            case 2:
                compressImage();
                break;
            case 3:
                filterImages();
                break;
            case 4:
                setPageSize();
                break;
            case 5:
                previewPDF();
                break;
            case 6:
                addBorder();
                break;
            case 7:
                rearrangeImages();
        }
    }

    private void rearrangeImages() {
        Intent intent = new Intent(mActivity, RearrangeImages.class);
        intent.putStringArrayListExtra(PREVIEW_IMAGES, mImagesUri);
        startActivityForResult(intent, INTENT_REQUEST_REARRANGE_IMAGE);
    }

    private void addBorder() {
        final MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                .title(getString(R.string.border))
                .customView(R.layout.dialog_border_image, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog1, which) -> {
                    View view = dialog1.getCustomView();
                    final EditText input = view.findViewById(R.id.border_width);
                    int value = 0;
                    try {
                        value = Integer.parseInt(String.valueOf(input.getText()));
                        if (value > 200 || value < 0) {
                            showSnackbar(mActivity, R.string.invalid_entry);
                        } else {
                            mPdfOptions.setBorderWidth(value);
                            showBorderWidth();
                        }
                    } catch (NumberFormatException e) {
                        showSnackbar(mActivity, R.string.invalid_entry);
                    }
                    final CheckBox cbSetDefault = view.findViewById(R.id.cbSetDefault);
                    if (cbSetDefault.isChecked()) {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putInt(Constants.DEFAULT_IMAGE_BORDER_TEXT, value);
                        editor.apply();
                    }
                }).build();
        dialog.show();
    }

    private void previewPDF() {
        Intent intent = new Intent(mActivity, PreviewActivity.class);
        intent.putExtra(PREVIEW_IMAGES, mImagesUri);
        startActivityForResult(intent, INTENT_REQUEST_PREVIEW_IMAGE);
    }

    private void compressImage() {
        String title = getString(R.string.compress_image) + " " +
                mSharedPreferences.getInt(DEFAULT_COMPRESSION, 30) + "%)";

        final MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                .title(title)
                .customView(R.layout.compress_image_dialog, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog1, which) -> {
                    final EditText qualityInput = dialog1.getCustomView().findViewById(R.id.quality);
                    final CheckBox cbSetDefault = dialog1.getCustomView().findViewById(R.id.cbSetDefault);

                    int check;
                    try {
                        check = Integer.parseInt(String.valueOf(qualityInput.getText()));
                        if (check > 100 || check < 0) {
                            showSnackbar(mActivity, R.string.invalid_entry);
                        } else {
                            mPdfOptions.setQualityString(String.valueOf(check));
                            if (cbSetDefault.isChecked()) {
                                SharedPreferences.Editor editor = mSharedPreferences.edit();
                                editor.putInt(DEFAULT_COMPRESSION, check);
                                editor.apply();
                            }
                            showCompression();
                        }
                    } catch (NumberFormatException e) {
                        showSnackbar(mActivity, R.string.invalid_entry);
                    }
                }).build();

        final View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        final EditText qualityValueInput = dialog.getCustomView().findViewById(R.id.quality);
        qualityValueInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        positiveAction.setEnabled(s.toString().trim().length() > 0);
                    }

                    @Override
                    public void afterTextChanged(Editable input) {
                    }
                });
        dialog.show();
        positiveAction.setEnabled(false);
    }

    private void passwordProtectPDF() {
        final MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.set_password)
                .customView(R.layout.custom_dialog, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.remove_dialog)
                .build();

        final View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        final View neutralAction = dialog.getActionButton(DialogAction.NEUTRAL);
        final EditText passwordInput = dialog.getCustomView().findViewById(R.id.password);
        passwordInput.setText(mPdfOptions.getPassword());
        passwordInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        positiveAction.setEnabled(s.toString().trim().length() > 0);
                    }

                    @Override
                    public void afterTextChanged(Editable input) {
                        if (StringUtils.isEmpty(input)) {
                            showSnackbar(mActivity, R.string.snackbar_password_cannot_be_blank);
                        } else {
                            mPdfOptions.setPassword(input.toString());
                            mPdfOptions.setPasswordProtected(true);
                            onPasswordAdded();
                        }
                    }
                });
        if (StringUtils.isNotEmpty(mPdfOptions.getPassword())) {
            neutralAction.setOnClickListener(v -> {
                onPasswordRemoved();
                mPdfOptions.setPasswordProtected(false);
                dialog.dismiss();
                showSnackbar(mActivity, R.string.password_remove);
            });
        }
        dialog.show();
        positiveAction.setEnabled(false);
    }

    private void showCompression() {
        mEnhancementOptionsEntityArrayList.get(2)
                .setName(mPdfOptions.getQualityString() + "% Compressed");
        mEnhancementOptionsAdapter.notifyDataSetChanged();
    }

    private void onPasswordAdded() {
        mEnhancementOptionsEntityArrayList.get(0)
                .setImage(getResources().getDrawable(R.drawable.baseline_done_24));
        mEnhancementOptionsAdapter.notifyDataSetChanged();
    }

    private void onPasswordRemoved() {
        mEnhancementOptionsEntityArrayList.get(0)
                .setImage(getResources().getDrawable(R.drawable.baseline_enhanced_encryption_24));
        mEnhancementOptionsAdapter.notifyDataSetChanged();
    }

    private void showBorderWidth() {
        mEnhancementOptionsEntityArrayList.get(6)
                .setName("Border width : " + mPdfOptions.getBorderWidth());
        mEnhancementOptionsAdapter.notifyDataSetChanged();
    }

    private void setPageSize() {
        PageSizeUtils utils = new PageSizeUtils(mActivity);
        utils.showPageSizeDialog(R.layout.set_page_size_dialog, false);
    }

    @Override
    public void onPDFCreated(boolean success, String path) {
        mImagesUri.clear();
        mNoOfImages.setVisibility(View.GONE);
        mImageCounter = 0;
        mPdfOptions = new ImageToPDFOptions();
        if (success) {
            mOpenPdf.setVisibility(View.VISIBLE);
            mMorphButtonUtility.morphToSuccess(mCreatePdf);
            onPasswordRemoved();
            mPath = path;
        }
    }

    private void cropNextImage() {
        if (mImageCounter != mImagesUri.size() && mImageCounter < mImagesUri.size()) {
            CropImage.activity(Uri.fromFile(new File(mImagesUri.get(mImageCounter))))
                    .setActivityMenuIconColor(mMorphButtonUtility.color(R.color.colorPrimary))
                    .setInitialCropWindowPaddingRatio(0)
                    .setAllowRotation(true)
                    .setActivityTitle(getString(R.string.cropImage_activityTitle) + (mImageCounter + 1))
                    .start(mActivity, this);
        } else {
            mImageCounter = 0;
        }
    }

    private boolean getRuntimePermissions(boolean openImagesActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) ||
                    (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED)) {
                mOpenSelectImages = openImagesActivity; // if We want next activity to open after getting permissions
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT);
                return false;
            }
        }
        return true;
    }

    /**
     * Opens Matisse activity to select Images
     */
    private void selectImages() {
        Matisse.from(this)
                .choose(MimeType.ofImage())
                .countable(true)
                .theme(R.style.imagePickerTheme)
                .maxSelectable(100)
                .forResult(INTENT_REQUEST_GET_IMAGES);
    }
}
