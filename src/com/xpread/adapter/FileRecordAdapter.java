
package com.xpread.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.uc.base.wa.WaEntry;
import com.xpread.R;
import com.xpread.SearchFriendActivity;
import com.xpread.control.Controller;
import com.xpread.provider.FileBean;
import com.xpread.provider.History;
import com.xpread.provider.RecordItem;
import com.xpread.swipelistview.SwipeListView;
import com.xpread.util.Const;
import com.xpread.util.FileUtil;
import com.xpread.util.Utils;
import com.xpread.wa.WaKeys;
import com.xpread.widget.RoundImageView;

public class FileRecordAdapter extends BaseAdapter {
    LayoutInflater mLayoutInflater;

    Context mContext;

    SwipeListView mListView;

    List<RecordItem> mRecordList;

    List<Bitmap> mFileIconList;

    ExecutorService executorService;

    DismissListener mDismissListener;

    public interface DismissListener {
        public void onItemDismiss(int postion);

        public void onItemAdded(int postion, String fileName);
    }

    int[] photos = Utils.photos;

    public FileRecordAdapter(Context context, SwipeListView listView, List<RecordItem> recordList,
            List<Bitmap> fileIconList) {
        super();
        executorService = Executors.newFixedThreadPool(1);

        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mContext = context;

        mListView = listView;

        mRecordList = recordList;

        mFileIconList = fileIconList;

    }

    public void setDismissListener(DismissListener listener) {
        mDismissListener = listener;
    }

    public void setData(List<RecordItem> data) {
        mRecordList = data;
        notifyDataSetChanged();
        mListView.closeOpenedItems();
    }

    @Override
    public int getCount() {
        return mRecordList == null ? 0 : mRecordList.size();
    }

    @Override
    public Object getItem(int position) {

        return mRecordList == null ? null : mRecordList.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder = null;
        int type = getItemViewType(position);
        if (convertView == null) {
            viewHolder = new ViewHolder();
            if (type == Const.SENDER) {
                convertView = mLayoutInflater.inflate(R.layout.send_record_item, parent, false);
            } else {
                convertView = mLayoutInflater.inflate(R.layout.receive_record_item, parent, false);
            }

            viewHolder.actionButton = (ImageButton)convertView.findViewById(R.id.open_file);
            viewHolder.shareButton = (ImageButton)convertView.findViewById(R.id.share_file);
            viewHolder.deleteButton = (ImageButton)convertView.findViewById(R.id.delete_file);
            viewHolder.deleteText = (TextView)convertView.findViewById(R.id.delete_confirm);
            viewHolder.stopText = (TextView)convertView.findViewById(R.id.stop_confirm);

            viewHolder.userIcon = (RoundImageView)convertView.findViewById(R.id.user_icon);
            viewHolder.fileIcon = (ImageView)convertView.findViewById(R.id.file_icon);
            viewHolder.fileName = (TextView)convertView.findViewById(R.id.file_name);
            viewHolder.fileSize = (TextView)convertView.findViewById(R.id.file_size);
            viewHolder.fileProgress = (TextView)convertView.findViewById(R.id.transfer_progress);
            viewHolder.fileSpeed = (TextView)convertView.findViewById(R.id.transfer_speed);
            viewHolder.fileBar = (ProgressBar)convertView.findViewById(R.id.transfer_bar);
            viewHolder.fileInfo = (RelativeLayout)convertView.findViewById(R.id.transfer_info);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        final RecordItem item = mRecordList.get(position);
        Bitmap fileIcon = mFileIconList == null ? null : mFileIconList.get(position);

        if (type == Const.SENDER) {
            viewHolder.userIcon.setImageResource(photos[Utils.getOwerIcon(mContext)]);
        } else {
            viewHolder.userIcon.setImageResource(photos[item.targetIcon]);
        }

        if (fileIcon != null) {
            viewHolder.fileIcon.setImageBitmap(fileIcon);
        } else {
            switch (item.type) {
                case Const.TYPE_APP:
                    viewHolder.fileIcon.setImageResource(R.drawable.app);
                    break;
                case Const.TYPE_IMAGE:
                    viewHolder.fileIcon.setImageResource(R.drawable.image);
                    break;
                case Const.TYPE_MUSIC:
                    viewHolder.fileIcon.setImageResource(R.drawable.music);
                    break;
                case Const.TYPE_VIDEO:
                    viewHolder.fileIcon.setImageResource(R.drawable.video);
                    break;
                case Const.TYPE_TEXT:
                    viewHolder.fileIcon.setImageResource(R.drawable.txt);
                    break;
                case Const.TYPE_ZIP:
                    viewHolder.fileIcon.setImageResource(R.drawable.zip);
                    break;
                default:
                    viewHolder.fileIcon.setImageResource(R.drawable.unknown);
                    break;
            }
        }

        viewHolder.fileName.setText(item.fileName);

        if (item.status == Const.FILE_TRANSFER_DOING) {
            viewHolder.stopText.setVisibility(View.VISIBLE);
            viewHolder.actionButton.setVisibility(View.GONE);
            viewHolder.shareButton.setVisibility(View.GONE);
            viewHolder.deleteButton.setVisibility(View.GONE);
            viewHolder.deleteText.setVisibility(View.GONE);

            viewHolder.fileInfo.setVisibility(View.VISIBLE);
            viewHolder.fileSize.setVisibility(View.GONE);

            viewHolder.fileBar.setProgress(item.progress);
            viewHolder.fileProgress.setText(String.format(
                    mContext.getString(R.string.current_progress), item.progress));

            float v = item.speed / Const.KILO;

            if (v >= Const.KILO) {
                v /= Const.KILO;
                viewHolder.fileSpeed
                        .setText(String.format(mContext.getString(R.string.speed_MB), v));
            } else {
                viewHolder.fileSpeed
                        .setText(String.format(mContext.getString(R.string.speed_KB), v));
            }

        } else {
            viewHolder.stopText.setVisibility(View.GONE);
            viewHolder.actionButton.setVisibility(View.VISIBLE);
            viewHolder.shareButton.setVisibility(View.VISIBLE);
            viewHolder.deleteButton.setVisibility(View.VISIBLE);
            viewHolder.deleteText.setVisibility(View.GONE);

            viewHolder.fileInfo.setVisibility(View.GONE);
            viewHolder.fileSize.setVisibility(View.VISIBLE);

            if (item.status == Const.FILE_TRANSFER_COMPLETE) {
                viewHolder.actionButton.setEnabled(true);
                viewHolder.shareButton.setEnabled(true);

                viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                        R.color.record_file_size));
                float size = item.size / Const.KILO;
                if (size >= Const.KILO) {
                    size /= Const.KILO;
                    viewHolder.fileSize.setText(String.format(mContext.getString(R.string.size_MB),
                            size));
                } else {
                    viewHolder.fileSize.setText(String.format(mContext.getString(R.string.size_KB),
                            (int)size));
                }

            } else {
                if (item.role == Const.RECEIVER) {
                    viewHolder.actionButton.setEnabled(false);
                    viewHolder.shareButton.setEnabled(false);
                } else {
                    viewHolder.actionButton.setEnabled(true);
                    viewHolder.shareButton.setEnabled(true);
                }

                if (item.status == Const.FILE_TRANSFER_PREPARED) {

                    viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                            R.color.record_to_be_sent));
                    viewHolder.fileSize.setText(R.string.to_be_transfer);
                } else if (item.status == Const.FILE_TRANSFER_FAILURE) {

                    viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                            R.color.record_transmission_error));
                    viewHolder.fileSize.setText(R.string.transfer_error);
                } else if (item.status == Const.FILE_TRANSFER_CANCEL) {

                    viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                            R.color.record_transmission_error));
                    viewHolder.fileSize.setText(R.string.user_canceled);
                }
            }

        }

        viewHolder.stopText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Controller.getInstance(mContext).cancelTransferFile(item.filePath);
                mListView.closeOpenedItems();
                updateViewState(position, Const.FILE_TRANSFER_CANCEL);
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD,
                        WaKeys.KEY_XPREAD_RECORD_DELETE_COMFIRM_OTHER);
            }
        });

        viewHolder.actionButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_OPEN_SUCESS);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                File file = new File(item.filePath);
                Uri uri = Uri.fromFile(file);
                if (!file.exists()) {
                    Toast.makeText(
                            mContext,
                            String.format(mContext.getResources()
                                    .getString(R.string.file_not_found), item.fileName),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                switch (item.type) {
                    case Const.TYPE_APP:
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        break;
                    case Const.TYPE_IMAGE:
                        intent.addCategory("android.intent.category.DEFAULT");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setDataAndType(uri, "image/*");
                        break;
                    case Const.TYPE_MUSIC:
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("oneshot", 0);
                        intent.putExtra("configchange", 0);
                        intent.setDataAndType(uri, "audio/*");
                        break;
                    case Const.TYPE_VIDEO:
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("oneshot", 0);
                        intent.putExtra("configchange", 0);
                        intent.setDataAndType(uri, "video/*");
                        break;
                    case Const.TYPE_TEXT:
                        intent.addCategory("android.intent.category.DEFAULT");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setDataAndType(uri, "text/plain");
                    case Const.TYPE_ZIP:
                        break;
                    default:
                        break;
                }

                try {
                    mContext.startActivity(intent);
                } catch (RuntimeException re) {
                    Toast.makeText(mContext, R.string.file_open_not_support, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        viewHolder.shareButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_SHARE_SUCESS);

                Controller controller = Controller.getInstance(mContext);

                FileBean fileBean = new FileBean();
                fileBean.uri = item.filePath;
                fileBean.type = item.type;
                fileBean.fileName = item.fileName;

                if (item.role == Const.RECEIVER
                        && !FileUtil.isFileExist(item.filePath, item.size)) {
                    Toast.makeText(
                            mContext,
                            String.format(mContext.getResources()
                                    .getString(R.string.file_not_found), item.fileName),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                List<FileBean> list = new ArrayList<FileBean>();
                list.add(fileBean);

                controller.preTransferFiles(list);
                if (controller.isConnected()) {
                    controller.handleTransferFiles();

                    if (mDismissListener != null) {
                        mDismissListener.onItemAdded(0, item.filePath);
                    }

                    RecordItem shareItem = new RecordItem();
                    shareItem.fileName = item.fileName;
                    shareItem.filePath = item.filePath;
                    shareItem.role = Const.SENDER;
                    shareItem.size = item.size;
                    shareItem.type = item.type;
                    shareItem.targetIcon = item.targetIcon;
                    mRecordList.add(0, shareItem);

                    notifyDataSetChanged();
                    mListView.closeOpenedItems();
                    controller.sendFiles();

                } else {
                    Intent intent = new Intent(mContext, SearchFriendActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                }
            }
        });

        final ViewHolder holder = viewHolder;
        viewHolder.deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                holder.deleteButton.setVisibility(View.GONE);
                holder.deleteText.setVisibility(View.VISIBLE);

                if (item.status == Const.FILE_TRANSFER_COMPLETE) {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_DELETE_SUCESS);
                } else {
                    WaEntry.statEpv(WaKeys.CATEGORY_XPREAD, WaKeys.KEY_XPREAD_RECORD_DELETE_OTHER);
                }
            }
        });

        viewHolder.deleteText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                holder.deleteText.setVisibility(View.GONE);
                mContext.getContentResolver().delete(
                        History.RecordsColumns.CONTENT_URI,
                        History.RecordsColumns.DATA + " = ? AND "
                                + History.RecordsColumns.TIME_STAMP + " = ? ", new String[] {
                                item.filePath, String.valueOf(item.time)
                        });

                mListView.closeOpenedItems();
                mRecordList.remove(position);
                notifyDataSetChanged();

                if (mDismissListener != null) {
                    mDismissListener.onItemDismiss(position);
                }

                executorService.execute(new Runnable() {

                    @Override
                    public void run() {

                        if (item.role == Const.RECEIVER) {
                            File file = new File(item.filePath);
                            if (file.exists()) {
                                file.delete();
                            }
                        }

                        if (item.status == Const.FILE_TRANSFER_COMPLETE) {
                            WaEntry.statEpv(WaKeys.CATEGORY_XPREAD,
                                    WaKeys.KEY_XPREAD_RECORD_DELETE_COMFIRM_SUCESS);
                        } else {
                            WaEntry.statEpv(WaKeys.CATEGORY_XPREAD,
                                    WaKeys.KEY_XPREAD_RECORD_DELETE_COMFIRM_OTHER);
                        }
                    }
                });
            }
        });

        return convertView;
    }

    public void resetView(int pos) {
        int visiblePosition = mListView.getFirstVisiblePosition();

        if (pos < visiblePosition) {
            return;
        }

        View convertView = mListView.getChildAt(pos - visiblePosition);
        if (convertView == null) {
            return;
        }
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.deleteText = (TextView)convertView.findViewById(R.id.delete_confirm);
        viewHolder.deleteButton = (ImageButton)convertView.findViewById(R.id.delete_file);

        viewHolder.deleteText.setVisibility(View.GONE);
        viewHolder.deleteButton.setVisibility(View.VISIBLE);

    }

    public void updateViewState(int pos, int status) {
        int visiblePosition = mListView.getFirstVisiblePosition();

        RecordItem item = mRecordList.get(pos);

        if ((item.status == Const.FILE_TRANSFER_CANCEL || item.status == Const.FILE_TRANSFER_COMPLETE)
                && status == Const.FILE_TRANSFER_FAILURE) {
            return;
        }

        item.status = status;

        if (pos < visiblePosition) {
            return;
        }

        View convertView = mListView.getChildAt(pos - visiblePosition);
        if (convertView == null) {
            return;
        }

        ViewHolder viewHolder = new ViewHolder();

        viewHolder.actionButton = (ImageButton)convertView.findViewById(R.id.open_file);
        viewHolder.shareButton = (ImageButton)convertView.findViewById(R.id.share_file);
        viewHolder.deleteButton = (ImageButton)convertView.findViewById(R.id.delete_file);
        viewHolder.deleteText = (TextView)convertView.findViewById(R.id.delete_confirm);
        viewHolder.stopText = (TextView)convertView.findViewById(R.id.stop_confirm);

        viewHolder.fileSize = (TextView)convertView.findViewById(R.id.file_size);
        viewHolder.fileProgress = (TextView)convertView.findViewById(R.id.transfer_progress);
        viewHolder.fileSpeed = (TextView)convertView.findViewById(R.id.transfer_speed);
        viewHolder.fileBar = (ProgressBar)convertView.findViewById(R.id.transfer_bar);
        viewHolder.fileInfo = (RelativeLayout)convertView.findViewById(R.id.transfer_info);

        if (status == Const.FILE_TRANSFER_DOING) {
            viewHolder.stopText.setVisibility(View.VISIBLE);
            viewHolder.actionButton.setVisibility(View.GONE);
            viewHolder.shareButton.setVisibility(View.GONE);
            viewHolder.deleteButton.setVisibility(View.GONE);
            viewHolder.deleteText.setVisibility(View.GONE);

            viewHolder.fileInfo.setVisibility(View.VISIBLE);
            viewHolder.fileSize.setVisibility(View.GONE);

            viewHolder.fileBar.setProgress(item.progress);
            viewHolder.fileProgress.setText(String.format(
                    mContext.getString(R.string.current_progress), item.progress));

            float v = item.speed / Const.KILO;

            if (v >= Const.KILO) {
                v /= Const.KILO;
                viewHolder.fileSpeed
                        .setText(String.format(mContext.getString(R.string.speed_MB), v));
            } else {
                viewHolder.fileSpeed
                        .setText(String.format(mContext.getString(R.string.speed_KB), v));
            }
        } else {
            viewHolder.stopText.setVisibility(View.GONE);
            viewHolder.actionButton.setVisibility(View.VISIBLE);
            viewHolder.shareButton.setVisibility(View.VISIBLE);
            viewHolder.deleteButton.setVisibility(View.VISIBLE);
            viewHolder.deleteText.setVisibility(View.GONE);

            viewHolder.fileInfo.setVisibility(View.GONE);
            viewHolder.fileSize.setVisibility(View.VISIBLE);

            if (item.status == Const.FILE_TRANSFER_COMPLETE) {
                viewHolder.actionButton.setEnabled(true);
                viewHolder.shareButton.setEnabled(true);

                viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                        R.color.record_file_size));
                float size = item.size / Const.KILO;
                if (size >= Const.KILO) {
                    size /= Const.KILO;
                    viewHolder.fileSize.setText(String.format(mContext.getString(R.string.size_MB),
                            size));
                } else {
                    viewHolder.fileSize.setText(String.format(mContext.getString(R.string.size_KB),
                            (int)size));
                }
            } else {
                if (item.role == Const.RECEIVER) {
                    viewHolder.actionButton.setEnabled(false);
                    viewHolder.shareButton.setEnabled(false);
                } else {
                    viewHolder.actionButton.setEnabled(true);
                    viewHolder.shareButton.setEnabled(true);
                }

                if (item.status == Const.FILE_TRANSFER_PREPARED) {

                    viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                            R.color.record_to_be_sent));
                    viewHolder.fileSize.setText(R.string.to_be_transfer);
                } else if (item.status == Const.FILE_TRANSFER_FAILURE) {

                    viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                            R.color.record_transmission_error));
                    viewHolder.fileSize.setText(R.string.transfer_error);
                } else if (item.status == Const.FILE_TRANSFER_CANCEL) {

                    viewHolder.fileSize.setTextColor(mContext.getResources().getColor(
                            R.color.record_transmission_error));
                    viewHolder.fileSize.setText(R.string.user_canceled);
                }
            }

        }

        mListView.closeAnimate(pos);

    }

    public void updateViewTransferInfo(int pos, int progress, int speed) {
        int visiblePosition = mListView.getFirstVisiblePosition();

        if (pos < visiblePosition) {
            return;
        }

        RecordItem item = mRecordList.get(pos);

        if (item.status == Const.FILE_TRANSFER_PREPARED) {
            updateViewState(pos, Const.FILE_TRANSFER_DOING);
        }

        item.progress = progress;
        item.speed = speed;

        View convertView = mListView.getChildAt(pos - visiblePosition);
        if (convertView == null) {
            return;
        }
        
        ViewHolder viewHolder = (ViewHolder)convertView.getTag();
        viewHolder.fileProgress = (TextView)convertView.findViewById(R.id.transfer_progress);
        viewHolder.fileSpeed = (TextView)convertView.findViewById(R.id.transfer_speed);
        viewHolder.fileBar = (ProgressBar)convertView.findViewById(R.id.transfer_bar);

        viewHolder.fileBar.setProgress(item.progress);
        viewHolder.fileProgress.setText(String.format(
                mContext.getString(R.string.current_progress), item.progress));

        float v = item.speed / Const.KILO;

        if (v >= Const.KILO) {
            v /= Const.KILO;
            viewHolder.fileSpeed.setText(String.format(mContext.getString(R.string.speed_MB), v));
        } else {
            viewHolder.fileSpeed.setText(String.format(mContext.getString(R.string.speed_KB), v));
        }

    }

    @Override
    public int getItemViewType(int position) {
        int role = mRecordList.get(position).role;
        return role;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    class ViewHolder {

        RoundImageView userIcon;

        ImageView fileIcon;

        TextView fileName;

        RelativeLayout fileInfo;

        TextView fileSize;

        ProgressBar fileBar;

        TextView fileProgress;

        TextView fileSpeed;

        ImageButton shareButton;

        ImageButton actionButton;

        ImageButton deleteButton;

        TextView deleteText;

        TextView stopText;
    }

}
