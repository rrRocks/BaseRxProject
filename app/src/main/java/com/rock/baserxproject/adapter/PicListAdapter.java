package com.rock.baserxproject.adapter;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.rock.basemodel.baseui.adapter.BasicQuickAdapter;
import com.rock.basemodel.baseui.adapter.BasicViewHolder;
import com.rock.baserxproject.R;
import com.rock.baserxproject.bean.HttpBean;
import com.rock.baserxproject.bean.PictureListBean;
import com.rock.baserxproject.utils.ImageLoader;
import com.rock.baserxproject.view.ScaleImageView;

import java.util.List;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

/**
 * @author: ruan
 * @date: 2020/5/15
 */
public class PicListAdapter extends BasicQuickAdapter<HttpBean.DataBean, BasicViewHolder> {

    public PicListAdapter(@Nullable List data) {
        super(R.layout.item_picture_layout, data);
    }

    @Override
    protected void convert(BasicViewHolder helper, HttpBean.DataBean item) {
        super.convert(helper, item);
        ScaleImageView imageView = helper.getView(R.id.pic_image);
//        Bitmap bitmap = ImageLoader.load(mContext, item.getUrl());
        imageView.setInitSize(item.getWidth(), item.getHight());

//        DrawableRequestBuilder<String> thumbnailRequest = Glide.with(mContext).load(item.getUrl());
//        Glide.with(mContext)
//                .load(item.getUrl())
//                .thumbnail(thumbnailRequest)
//                .into(imageView);

//        Glide.with(mContext).load(item.getUrl()).into(imageView);
        //屏幕的宽度(px值）
        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        //Item的宽度，或图片的宽度
        int width = screenWidth/2;
        Glide.with(mContext)
                .load(item.getUrl())
                .override(width,SIZE_ORIGINAL)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(imageView);
        helper.addOnClickListener(R.id.pic_image);
    }
}
