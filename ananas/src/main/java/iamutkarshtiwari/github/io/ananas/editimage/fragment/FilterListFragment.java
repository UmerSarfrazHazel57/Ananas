package iamutkarshtiwari.github.io.ananas.editimage.fragment;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import iamutkarshtiwari.github.io.ananas.BaseActivity;
import iamutkarshtiwari.github.io.ananas.R;
import iamutkarshtiwari.github.io.ananas.editimage.EditImageActivity;
import iamutkarshtiwari.github.io.ananas.editimage.ModuleConfig;
import iamutkarshtiwari.github.io.ananas.editimage.adapter.FilterAdapter;
import iamutkarshtiwari.github.io.ananas.editimage.fliter.PhotoProcessing;
import iamutkarshtiwari.github.io.ananas.editimage.view.imagezoom.ImageViewTouchBase;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FilterListFragment extends BaseEditFragment {
    public static final int INDEX = ModuleConfig.INDEX_FILTER;
    public static final int NULL_FILTER_INDEX = 0;
    public static final String TAG = FilterListFragment.class.getName();

    private View mainView;
    private Bitmap filterBitmap;
    private Bitmap currentBitmap;
    private Dialog loadingDialog;
    private FilterAdapter filterAdapter;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static FilterListFragment newInstance() {
        return new FilterListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_edit_image_fliter, null);
        loadingDialog = BaseActivity.getLoadingDialog(getActivity(), R.string.iamutkarshtiwari_github_io_ananas_loading,
                false);
        return mainView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RecyclerView filterRecyclerView = mainView.findViewById(R.id.filter_recycler);
         filterAdapter = new FilterAdapter(this, getContext());
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        filterRecyclerView.setLayoutManager(layoutManager);
        filterRecyclerView.setAdapter(filterAdapter);

        View backBtn = mainView.findViewById(R.id.back_to_main);
        backBtn.setOnClickListener(v -> backToMain());
    }

    @Override
    public void onShow() {
        activity.mode = EditImageActivity.MODE_FILTER;
        activity.filterListFragment.setCurrentBitmap(activity.getMainBit());
        activity.mainImage.setImageBitmap(activity.getMainBit());
        activity.mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        activity.mainImage.setScaleEnabled(false);
        createThumbnail();
       // activity.bannerFlipper.showNext();
    }

    @Override
    public void backToMain() {
        currentBitmap = activity.getMainBit();
        filterBitmap = null;
        activity.mainImage.setImageBitmap(activity.getMainBit());
        activity.mode = EditImageActivity.MODE_NONE;
        activity.bottomGallery.setCurrentItem(0);
        activity.mainImage.setScaleEnabled(true);
        if(activity.bannerFlipper.getCurrentView().getId() != R.id.save_btn){
            activity.bannerFlipper.showPrevious();
        }

    }

    public void applyFilterImage() {
        if (currentBitmap == activity.getMainBit()) {
            backToMain();
        } else {
            activity.changeMainBitmap(filterBitmap, true);
            backToMain();
        }
    }

    @Override
    public void onDestroy() {
        tryRecycleFilterBitmap();
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void tryRecycleFilterBitmap() {
        if (filterBitmap != null && (!filterBitmap.isRecycled())) {
            filterBitmap.recycle();
        }
    }

    public void enableFilter(int filterIndex) {

        if(loadingDialog.isShowing()){
            return;
        }
        if (filterIndex == NULL_FILTER_INDEX) {
            activity.mainImage.setImageBitmap(activity.getMainBit());
            currentBitmap = activity.getMainBit();
            if(activity.bannerFlipper.getCurrentView().getId() != R.id.save_btn){
                activity.bannerFlipper.showPrevious();
            }
            return;
        }

        Disposable applyFilterDisposable = applyFilter(filterIndex)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> loadingDialog.show())
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(
                        this::updatePreviewWithFilter,
                        e -> showSaveErrorToast()
                );

        compositeDisposable.add(applyFilterDisposable);
    }


    public void createThumbnail() {
        Disposable applyFilterDisposable = createThumbnails()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(subscriber -> loadingDialog.show())
                .doFinally(() -> loadingDialog.dismiss())
                .subscribe(
                        this::updateThumbnails,
                        e -> showSaveErrorToast()
                );

        compositeDisposable.add(applyFilterDisposable);
    }


    private void updatePreviewWithFilter(Bitmap bitmapWithFilter) {
        if (bitmapWithFilter == null) return;

        if (filterBitmap != null && (!filterBitmap.isRecycled())) {
            filterBitmap.recycle();
        }

        filterBitmap = bitmapWithFilter;
        activity.mainImage.setImageBitmap(filterBitmap);
        currentBitmap = filterBitmap;
    }


    private void updateThumbnails(ArrayList<Bitmap> bitmapArrayList){
        filterAdapter.updateData(bitmapArrayList);

    }


    private void showSaveErrorToast() {
        Toast.makeText(getActivity(), R.string.iamutkarshtiwari_github_io_ananas_save_error, Toast.LENGTH_SHORT).show();
    }

    private Single<Bitmap> applyFilter(int filterIndex) {

        if(filterIndex == 0){
            if(activity.bannerFlipper.getCurrentView().getId() != R.id.save_btn){
                activity.bannerFlipper.showPrevious();
            }
        }else{
            if(activity.bannerFlipper.getCurrentView().getId() != R.id.apply){
                activity.bannerFlipper.showNext();
            }
        }


        return Single.fromCallable(() -> {

            Bitmap srcBitmap = Bitmap.createBitmap(activity.getMainBit().copy(
                    Bitmap.Config.RGB_565, true));
            return PhotoProcessing.filterPhoto(srcBitmap, filterIndex);
        });
    }




    private Single<ArrayList<Bitmap>> createThumbnails() {

        ArrayList<Bitmap> bitmapArrayList = new  ArrayList();

        return Single.fromCallable(() -> {
            for (int i = 0; i <=11 ; i++) {
                Bitmap srcBitmap = Bitmap.createScaledBitmap(activity.getMainBit().copy(
                        Bitmap.Config.RGB_565, true),200,300,false);
                if(i != NULL_FILTER_INDEX){
                    bitmapArrayList.add(PhotoProcessing.filterPhoto(srcBitmap, i));
                }else{
                    bitmapArrayList.add(srcBitmap);
                }
            }

            return bitmapArrayList;
        });
    }

    public void setCurrentBitmap(Bitmap currentBitmap) {
        this.currentBitmap = currentBitmap;
    }


}
