package com.silence.account.fragment;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bmob.BmobPro;
import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.silence.account.R;
import com.silence.account.activity.AboutActivity;
import com.silence.account.activity.LoginActivity;
import com.silence.account.activity.MainActivity;
import com.silence.account.activity.UserActivity;
import com.silence.account.application.AccountApplication;
import com.silence.account.dao.ExpenseCatDao;
import com.silence.account.dao.ExpenseDao;
import com.silence.account.dao.IncomeCatDao;
import com.silence.account.dao.IncomeDao;
import com.silence.account.model.Expense;
import com.silence.account.model.ExpenseCat;
import com.silence.account.model.Income;
import com.silence.account.model.IncomeCat;
import com.silence.account.model.User;
import com.silence.account.receiver.AlarmReceiver;
import com.silence.account.utils.Constant;
import com.silence.account.utils.DBOpenHelper;
import com.silence.account.utils.DateUtils;
import com.silence.account.utils.L;
import com.silence.account.utils.T;
import com.silence.account.view.CircleImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.OnClick;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.SaveListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;

/**
 * 我的模块
 */
public class MineFragment extends BaseFragment {

    @Bind(R.id.iv_user_photo)
    CircleImageView mIvUserPhoto;
    @Bind(R.id.me_username)
    TextView mUsername;
    private MainActivity mContext;
    private static final int UPDATE_USER = 0X1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        File photo = new File(BmobPro.getInstance(mContext).getCacheDownloadDir() + File.separator +
                BmobUser.getCurrentUser(mContext, User.class).getPicture());
        if (photo.exists()) {
            mIvUserPhoto.setImageBitmap(BitmapFactory.decodeFile(photo.getAbsolutePath()));
        }
        mUsername.setText(BmobUser.getCurrentUser(mContext).getUsername());
        return view;
    }

    @Override
    protected int getResId() {
        return R.layout.fragment_mine;
    }

    @Override
    protected Fragment getSubFragment() {
        return this;
    }

    SlideDateTimeListener listener = new SlideDateTimeListener() {
        @Override
        public void onDateTimeSet(Date date) {
            Intent intent = new Intent(mContext, AlarmReceiver.class);
            intent.setAction(Constant.ACTION_ALARM);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
            Toast.makeText(mContext, "闹钟将在" + DateUtils.date2Str(date, "MM-dd HH:mm")
                    + "发出提醒", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDateTimeCancel() {

        }
    };

    @OnClick({R.id.ll_me_user, R.id.ll_me_reminder, R.id.ll_me_share,
            R.id.ll_me_about, R.id.ll_me_check, R.id.ll_me_init, R.id.ll_me_sync})
    public void mineClick(View view) {
        switch (view.getId()) {
            case R.id.ll_me_user: {
                startActivityForResult(new Intent(mContext, UserActivity.class), UPDATE_USER);
            }
            break;
            case R.id.ll_me_reminder: {
                new SlideDateTimePicker.Builder(mContext.getSupportFragmentManager())
                        .setListener(listener)
                        .setIs24HourTime(true)
                        .setIndicatorColor(Color.parseColor("#f6a844"))
                        .build()
                        .show();
            }
            break;
            case R.id.ll_me_init: {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("警告");
                builder.setCancelable(true);
                builder.setMessage(" 初始化将删除所有的软件记录并恢复软件的最初设置，你确定这么做吗？");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //在这里初始化所有数据表，并退出登录
                        DBOpenHelper.getInstance(mContext).dropTable();
                        BmobUser.logOut(mContext);
                        AccountApplication.sUser = null;
                        startActivity(new Intent(mContext, LoginActivity.class));
                        mContext.finish();
                    }
                });
                builder.setNegativeButton("取消", null);
                builder.show();
            }
            break;
            case R.id.ll_me_check: {
                final ProgressDialog dialog = new ProgressDialog(mContext);
                dialog.setTitle("正在检查新版本");
                dialog.setMessage("请稍后...");
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        T.showShort(mContext, "已是最新版本，无需更新！");
                    }
                });
                dialog.show();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        dialog.cancel();
                    }
                };
                Timer timer = new Timer();
                timer.schedule(task, Constant.DELAY_TIME);
            }
            break;
            case R.id.ll_me_share: {
                ShareSDK.initSDK(getActivity(), "1a0ab86aab13c");
                OnekeyShare oks = new OnekeyShare();
                oks.disableSSOWhenAuthorize();
                oks.setTitle("分享一款好用的记账软件");
                oks.setTitleUrl("http://weibo.com/kindsuperliu");
                oks.setText("亲们，给大家推荐一款记账软件，好漂亮的界面，记账好简单，超赞的！");
                oks.setImageUrl("http://img.blog.csdn.net/20161220115033833?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvS2luZFN1cGVyX2xpdQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast");
                oks.setSite(getString(R.string.app_name));
                oks.setSiteUrl("http://weibo.com/kindsuperliu");
                oks.setUrl("http://weibo.com/kindsuperliu");
                oks.show(mContext);
            }
            break;
            case R.id.ll_me_about: {
                startActivity(new Intent(mContext, AboutActivity.class));
            }
            break;
            case R.id.ll_me_sync:
                //同步用户
                AccountApplication.sUser.save(getActivity(),saveListener);
                //获取支出种类
                ExpenseCatDao expenseCatDao = new ExpenseCatDao(getActivity());
                List<ExpenseCat> expenseCats = expenseCatDao.getExpenseCat(AccountApplication.sUser.getId());
                List<BmobObject> bmobExpenseCats = new ArrayList<>();
                for (int i = 0;i<expenseCats.size();i++){
                    bmobExpenseCats.add(expenseCats.get(i));
                }
                new BmobObject().insertBatch(getActivity(), bmobExpenseCats, saveListener);
                //获取收入种类
                IncomeCatDao incomeCatDao = new IncomeCatDao(getActivity());
                List<IncomeCat> incomeCats = incomeCatDao.getIncomeCat(AccountApplication.sUser.getId());
                List<BmobObject> bmobIncomeCats = new ArrayList<>();
                for (int i = 0;i<incomeCats.size();i++){
                    bmobIncomeCats.add(incomeCats.get(i));
                }
                new BmobObject().insertBatch(getActivity(), bmobIncomeCats, saveListener);
                //获取支出
                ExpenseDao expenseDao = new ExpenseDao(getActivity());
                List<Expense> expenses = expenseDao.getExpense();
                List<BmobObject> objects1 = new ArrayList<BmobObject>();
                for (int i = 0; i < expenses.size(); i++) {
                    L.e(expenses.get(i).toString());
                    objects1.add(expenses.get(i));
                }
                new BmobObject().insertBatch(getActivity(), objects1, saveListener);
                //获取收入
                IncomeDao incomeDao = new IncomeDao(getActivity());
                List<Income> incomes = incomeDao.getIncomes();
                List<BmobObject> objects2 = new ArrayList<BmobObject>();
                for (int i = 0; i < incomes.size(); i++) {
                    L.e(incomes.get(i).toString());
                    objects2.add(incomes.get(i));
                }
                new BmobObject().insertBatch(getActivity(), objects2, saveListener);

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == UPDATE_USER) {
            String newFile = data.getStringExtra(Constant.NEW_FILENAME);
            if (newFile != null) {
                mIvUserPhoto.setImageBitmap(BitmapFactory.decodeFile(BmobPro.getInstance(mContext)
                        .getCacheDownloadDir() + File.separator + newFile));
            }
            String newName = data.getStringExtra(Constant.NEW_USERNAME);
            if (newName != null) {
                mUsername.setText(newName);
            }
        }
    }

    private SaveListener saveListener = new SaveListener() {
        @Override
        public void onSuccess() {
            T.showShort(getActivity(),"同步成功");
        }

        @Override
        public void onFailure(int i, String s) {

        }
    };
}
