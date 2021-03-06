package com.silence.account.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.silence.account.R;
import com.silence.account.activity.CategoryAty;
import com.silence.account.adapter.GridInCatAdapter;
import com.silence.account.application.AccountApplication;
import com.silence.account.dao.IncomeCatDao;
import com.silence.account.dao.IncomeDao;
import com.silence.account.model.Income;
import com.silence.account.model.IncomeCat;
import com.silence.account.utils.Constant;
import com.silence.account.utils.DateUtils;
import com.silence.account.utils.L;
import com.silence.account.utils.T;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

public class IncomeFragment extends BaseFragment implements AdapterView.OnItemClickListener {

    @Bind(R.id.icon_income_cat)
    ImageView mIconIncomeCat;
    @Bind(R.id.label_income_cat)
    TextView mLabelIncomeCat;
    @Bind(R.id.et_income)
    EditText mEtIncome;
    @Bind(R.id.label_income_time)
    TextView mEtIncomeTime;
    @Bind(R.id.et_income_note)
    EditText mEtIncomeNote;
    @Bind(R.id.ll_income_cat)
    LinearLayout mLlIncomeCat;
    private static final int REQUEST_ADD_CATEGORY = 0x101;    //添加图标
    private static final int REQUEST_UPDATE_CATEGORY = 0x102;  //修改图标
    private boolean mIsUpdateCat;
    private IncomeCatDao mIncomeCatDao;
    private Date mDate;
    private Income mIncome;
    private PopupWindow mPopupWindow;
    private onTimePickListener mOnTimePickListener;
    private boolean mIsUpdateIncome;
    private GridInCatAdapter mCatAdapter;
    private Context mContext;


    public static IncomeFragment getInstance(Income income) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constant.RECORD, income);
        IncomeFragment incomeFragment = new IncomeFragment();
        incomeFragment.setArguments(bundle);
        return incomeFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //instanceof 判断左边的对象是否是右边类的实例
        if (activity instanceof onTimePickListener) {
            mOnTimePickListener = (onTimePickListener) activity;
        }
        mContext = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        View view = super.onCreateView(inflater,container,savedInstanceState);
        mIncomeCatDao = new IncomeCatDao(mContext);
        mCatAdapter = new GridInCatAdapter(mContext, getCategory());
        Bundle arguments = getArguments();
        //携带参数不为空，是更改操作
        if (arguments != null) {
            mIsUpdateIncome = true;
            mIncome = arguments.getParcelable(Constant.RECORD);
            mEtIncome.setText(String.valueOf(mIncome.getAmount()));
            mLabelIncomeCat.setText(mIncome.getCategory().getName());
            mIconIncomeCat.setImageResource(mIncome.getCategory().getImageId());
            mEtIncomeNote.setText(mIncome.getNote());
            mDate = mIncome.getDate();
        } else {
            mIsUpdateIncome = false;
            mIncome = new Income();
            mDate = new Date();
            mIncome.setDate(mDate);
            mIncome.setUser(AccountApplication.sUser);
            mIncome.setCategory((IncomeCat) mCatAdapter.getItem(0));
        }
        mEtIncomeTime.setText(DateUtils.date2Str(mDate));

        //绘制弹出页的界面
        LinearLayout linearLayout = (LinearLayout) getActivity().getLayoutInflater().
                inflate(R.layout.pop_category, null);
        GridView gridIncomeCat = (GridView) linearLayout.findViewById(R.id.grid_category);
        gridIncomeCat.setAdapter(mCatAdapter);
        gridIncomeCat.setOnItemClickListener(this);
        //为类别图标设置点击监听事件，点击的时候设置账目的类别
        gridIncomeCat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mCatAdapter.getCloseVisibility() == View.VISIBLE) {
                    mCatAdapter.setCloseVisibility(View.GONE);
                }
                return false;
            }
        });
        //为类别图标设置长按监听事件，长按的时候进入修改类别的界面
        gridIncomeCat.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                IncomeCat incomeCat = (IncomeCat) mCatAdapter.getItem(position);
                Intent intent = new Intent(mContext, CategoryAty.class);
                intent.putExtra(Constant.UPDATE_CAT, (Parcelable) incomeCat);
                startActivityForResult(intent, REQUEST_UPDATE_CATEGORY);
                return true;
            }
        });
        //创建弹出式对话框，让用户管理收支类别
        mPopupWindow = new PopupWindow(linearLayout, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0xb0000000));
        mPopupWindow.setAnimationStyle(R.style.popwindow_anim_style);
        return view;
    }

    @Override
    protected int getResId() {
        return R.layout.fragment_income;
    }

    @Override
    protected Fragment getSubFragment() {
        return this;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsUpdateCat && mCatAdapter != null) {
            mCatAdapter.setData(getCategory());
        }
    }

    private List<IncomeCat> getCategory() {
        List<IncomeCat> cats = mIncomeCatDao.getIncomeCat(AccountApplication.sUser.getId());
        cats.add(new IncomeCat(R.mipmap.jiahao_bai, "添加", AccountApplication.sUser));
        cats.add(new IncomeCat(R.mipmap.jianhao_bai, "删除", AccountApplication.sUser));
        return cats;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            mIsUpdateCat = requestCode == REQUEST_ADD_CATEGORY || requestCode == REQUEST_UPDATE_CATEGORY;
        } else {
            mIsUpdateCat = false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        IncomeCat incomeCat = (IncomeCat) parent.getItemAtPosition(position);
        //如果点击的是增加
        if (incomeCat.getImageId() == R.mipmap.jiahao_bai) {
            Intent intent = new Intent(mContext, CategoryAty.class);
            intent.putExtra(Constant.TYPE_CATEGORY, Constant.TYPE_INCOME);
            startActivityForResult(intent, REQUEST_ADD_CATEGORY);
            //如果点击的是减少
        } else if (incomeCat.getImageId() == R.mipmap.jianhao_bai) {
            mCatAdapter.setCloseVisibility(View.VISIBLE);
        } else {
            mIncome.setCategory(incomeCat);
            mIconIncomeCat.setImageResource(incomeCat.getImageId());
            mLabelIncomeCat.setText(incomeCat.getName());
            mPopupWindow.dismiss();
        }
    }

    @OnClick({R.id.label_income_time, R.id.icon_income_speak, R.id.ll_income_cat, R.id.btn_income_save})
    public void incomeClick(View view) {
        switch (view.getId()) {
            case R.id.label_income_time: {
                if (mOnTimePickListener != null) {
                    mOnTimePickListener.DisplayDialog(mDate);
                }
            }
            break;
            case R.id.ll_income_cat:
                if (mPopupWindow.isShowing()) {
                    mPopupWindow.dismiss();
                } else {
                    mPopupWindow.showAsDropDown(mLlIncomeCat);
                }
                break;
            case R.id.btn_income_save: {
                saveIncome();
            }
            break;
            //语音输入
            case R.id.icon_income_speak: {
                RecognizerDialog mDialog = new RecognizerDialog(mContext, null);
                mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
                mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
                final StringBuilder stringBuilder = new StringBuilder();
                mDialog.setListener(new RecognizerDialogListener() {
                    @Override
                    public void onResult(RecognizerResult recognizerResult, boolean b) {
                        try {
                            JSONObject jsonObject = new JSONObject(recognizerResult.getResultString());
                            JSONArray jsonArray = jsonObject.getJSONArray("ws");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONArray cw = jsonArray.getJSONObject(i).getJSONArray("cw");
                                JSONObject w = cw.getJSONObject(0);
                                stringBuilder.append(w.get("w"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mEtIncomeNote.setText(stringBuilder);
                    }

                    @Override
                    public void onError(SpeechError speechError) {
                        L.i(speechError.getPlainDescription(true));
                    }
                });
                mDialog.show();
            }
            break;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnTimePickListener = null;
    }

    private void saveIncome() {
        String trim = mEtIncome.getText().toString().trim();
        if (TextUtils.isEmpty(trim)) {
            Toast.makeText(mContext, "请输入金额", Toast.LENGTH_SHORT).show();
            return;
        }
        float amount = Float.parseFloat(trim);
        String note = mEtIncomeNote.getText().toString().trim();
        mIncome.setAmount(amount);
        mIncome.setNote(note);
        IncomeDao incomeDao = new IncomeDao(mContext);
        if (!mIsUpdateIncome) {
            if (incomeDao.addIncome(mIncome)) {
                T.showShort(mContext, "保存成功");
                EventBus.getDefault().post("income_inserted");
                getActivity().finish();
            } else {
                T.showShort(mContext, "保存失败");
            }
        } else {
            if (incomeDao.updateIncome(mIncome)) {
                T.showShort(mContext, "修改成功");
                EventBus.getDefault().post("income_updated");
                getActivity().finish();
            } else {
                T.showShort(mContext, "修改失败");
            }
        }
    }

    public void setDate(Date date) {
        mIncome.setDate(date);
        mEtIncomeTime.setText(DateUtils.date2Str(date));
    }

    public interface onTimePickListener {
        void DisplayDialog(Date date);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    //解决删除图标后fragment仍然有该图标
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshIncomeCat(IncomeCat incomeCat){
        if (incomeCat.getName().equals(mLabelIncomeCat.getText().toString())){
            mLabelIncomeCat.setText("工资");
            mIconIncomeCat.setImageResource(R.mipmap.icon_shouru_type_gongzi);
        }

    }
}
