package io.fluentic.ubicdn.info_list.holder;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;

import io.fluentic.ubicdn.R;
import io.fluentic.ubicdn.extractor.InfoItem;
import io.fluentic.ubicdn.extractor.stream.StreamInfoItem;
import io.fluentic.ubicdn.extractor.stream.StreamType;
import io.fluentic.ubicdn.info_list.InfoItemBuilder;
import io.fluentic.ubicdn.util.Localization;

public class StreamMiniInfoItemHolder extends InfoItemHolder {

    public final ImageView itemThumbnailView;
    public final TextView itemVideoTitleView;
    public final TextView itemUploaderView;
    public final TextView itemDurationView;

    StreamMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
        itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
        itemDurationView = itemView.findViewById(R.id.itemDurationView);
    }

    public StreamMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_stream_mini_item, parent);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem) {
        if (!(infoItem instanceof StreamInfoItem)) return;
        final StreamInfoItem item = (StreamInfoItem) infoItem;

        itemVideoTitleView.setText(item.name);
        itemUploaderView.setText(item.uploader_name);

        if (item.duration > 0) {
            itemDurationView.setText(Localization.getDurationString(item.duration));
            itemDurationView.setBackgroundColor(ContextCompat.getColor(itemBuilder.getContext(), R.color.duration_background_color));
            itemDurationView.setVisibility(View.VISIBLE);
        } else if (item.stream_type == StreamType.LIVE_STREAM) {
            itemDurationView.setText(R.string.duration_live);
            itemDurationView.setBackgroundColor(ContextCompat.getColor(itemBuilder.getContext(), R.color.live_duration_background_color));
            itemDurationView.setVisibility(View.VISIBLE);
        } else {
            itemDurationView.setVisibility(View.GONE);
        }

        // Default thumbnail is shown on error, while loading and if the url is empty
        itemBuilder.getImageLoader()
                .displayImage(item.thumbnail_url, itemThumbnailView, StreamInfoItemHolder.DISPLAY_THUMBNAIL_OPTIONS);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemBuilder.getOnStreamSelectedListener() != null) {
                    itemBuilder.getOnStreamSelectedListener().selected(item);
                }
            }
        });
    }

    /**
     * Display options for stream thumbnails
     */
    public static final DisplayImageOptions DISPLAY_THUMBNAIL_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cloneFrom(BASE_DISPLAY_IMAGE_OPTIONS)
                    .showImageOnFail(R.drawable.dummy_thumbnail)
                    .showImageForEmptyUri(R.drawable.dummy_thumbnail)
                    .showImageOnLoading(R.drawable.dummy_thumbnail)
                    .build();
}
