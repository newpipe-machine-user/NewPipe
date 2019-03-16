package org.schabi.newpipe.info_list.holder;

import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.info_list.InfoItemBuilder;
import org.schabi.newpipe.report.ErrorActivity;
import org.schabi.newpipe.util.CommentTextOnTouchListener;
import org.schabi.newpipe.util.ImageDisplayConstants;
import org.schabi.newpipe.util.NavigationHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentsMiniInfoItemHolder extends InfoItemHolder {
    public final CircleImageView itemThumbnailView;
    private final TextView itemContentView;
    private final TextView itemLikesCountView;
    private final TextView itemDislikesCountView;
    private final TextView itemPublishedTime;

    private static final int commentDefaultLines = 2;
    private static final int commentExpandedLines = 1000;

    private String commentText;
    private String streamUrl;

    private static final Pattern pattern = Pattern.compile("(\\d+:)?(\\d+)?:(\\d+)");

    private final Linkify.TransformFilter timestampLink = new Linkify.TransformFilter() {
        @Override
        public String transformUrl(Matcher match, String url) {
            int timestamp = 0;
            String hours = match.group(1);
            String minutes = match.group(2);
            String seconds = match.group(3);
            if(hours != null) timestamp += (Integer.parseInt(hours.replace(":", ""))*3600);
            if(minutes != null) timestamp += (Integer.parseInt(minutes.replace(":", ""))*60);
            if(seconds != null) timestamp += (Integer.parseInt(seconds));
            return streamUrl + url.replace(match.group(0), "&t=" + String.valueOf(timestamp));
        }
    };

    CommentsMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemLikesCountView = itemView.findViewById(R.id.detail_thumbs_up_count_view);
        itemDislikesCountView = itemView.findViewById(R.id.detail_thumbs_down_count_view);
        itemPublishedTime = itemView.findViewById(R.id.itemPublishedTime);
        itemContentView = itemView.findViewById(R.id.itemCommentContentView);
    }

    public CommentsMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_comments_mini_item, parent);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem) {
        if (!(infoItem instanceof CommentsInfoItem)) return;
        final CommentsInfoItem item = (CommentsInfoItem) infoItem;

        itemBuilder.getImageLoader()
                .displayImage(item.getAuthorThumbnail(),
                        itemThumbnailView,
                        ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS);

        itemThumbnailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    boolean acceptsChannelUrl = NewPipe.getService(item.getServiceId()).getChannelLHFactory().acceptUrl(item.getAuthorEndpoint());
                    if(acceptsChannelUrl){
                        final AppCompatActivity activity = (AppCompatActivity) itemBuilder.getContext();
                        NavigationHelper.openChannelFragment(
                                activity.getSupportFragmentManager(),
                                item.getServiceId(),
                                item.getAuthorEndpoint(),
                                item.getAuthorName());
                    }else{
                        NavigationHelper.openLinkInBrowser(itemBuilder.getContext(), item.getAuthorEndpoint());
                    }
                } catch (Exception e) {
                    ErrorActivity.reportUiError((AppCompatActivity) itemBuilder.getContext(), e);
                }
            }
        });

        streamUrl = item.getUrl();

        itemContentView.setMaxLines(commentDefaultLines);
        commentText = item.getCommentText();
        itemContentView.setText(commentText);
        linkify();
        itemContentView.setOnTouchListener(CommentTextOnTouchListener.INSTANCE);

        if(itemContentView.getLineCount() == 0){
            itemContentView.post(() -> ellipsize());
        }else{
            ellipsize();
        }

        if (null != item.getLikeCount()) {
            itemLikesCountView.setText(String.valueOf(item.getLikeCount()));
        }
        itemPublishedTime.setText(item.getPublishedTime());

        itemView.setOnClickListener(view -> {
            toggleEllipsize();
            if (itemBuilder.getOnCommentsSelectedListener() != null) {
                itemBuilder.getOnCommentsSelectedListener().selected(item);
            }
        });
    }

    private void ellipsize() {
        if (itemContentView.getLineCount() > commentDefaultLines){
            int endOfLastLine = itemContentView.getLayout().getLineEnd(commentDefaultLines - 1);
            String newVal = itemContentView.getText().subSequence(0, endOfLastLine - 3) + "...";
            itemContentView.setText(newVal);
            linkify();
        }
    }

    private void toggleEllipsize() {
        if (itemContentView.getText().toString().equals(commentText)) {
            ellipsize();
        } else {
            expand();
        }
    }

    private void expand() {
        itemContentView.setMaxLines(commentExpandedLines);
        itemContentView.setText(commentText);
        linkify();
    }

    private void linkify(){
        Linkify.addLinks(itemContentView, Linkify.WEB_URLS);
        Linkify.addLinks(itemContentView, pattern, null, null, timestampLink);
        itemContentView.setMovementMethod(null);
    }
}
