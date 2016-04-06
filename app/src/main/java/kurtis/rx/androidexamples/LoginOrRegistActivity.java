package com.iqianjin.client.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.iqianjin.client.AppData;
import com.iqianjin.client.Encryption.RSAUtil;
import com.iqianjin.client.MyApplication;
import com.iqianjin.client.R;
import com.iqianjin.client.http.HttpClientUtils;
import com.iqianjin.client.http.ReqParam;
import com.iqianjin.client.http.ServerAddr;
import com.iqianjin.client.manager.UserInfoManager;
import com.iqianjin.client.model.IqianjinPublicModel;
import com.iqianjin.client.model.UserInfo;
import com.iqianjin.client.protocol.BaseResponse;
import com.iqianjin.client.protocol.CommModelResponse;
import com.iqianjin.client.protocol.RegistImgResponse;
import com.iqianjin.client.utils.AlisaHandler;
import com.iqianjin.client.utils.AnnotationRes.NoRes;
import com.iqianjin.client.utils.AppStatisticsUtil;
import com.iqianjin.client.utils.Constants;
import com.iqianjin.client.utils.ErrorUtils;
import com.iqianjin.client.utils.H5Type;
import com.iqianjin.client.utils.Util;
import com.iqianjin.client.utils.ViewShakeUtils;
import com.iqianjin.client.view.KeyboardLayout;
import com.iqianjin.client.view.LockPatternUtils;
import com.puhuifinance.libs.http.JsonHttpResponseHandler;
import com.puhuifinance.libs.xutil.ThreadUtil;
import com.puhuifinance.libs.xutil.XLog;

import org.apache.http.Header;
import org.json.JSONObject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Created by Han on 2015/12/21.
 */
public class LoginOrRegistActivity extends BaseActivity {
    private boolean isLogin = false;

    private ScrollView mScrollView;
    private RelativeLayout mPasswordShowRl;
    private ImageView mPasswordShowIv;
    private boolean isShowPwd = false;

    private LinearLayout mTitleLayout;
    private LinearLayout mainLinearLayout;
    private KeyboardLayout mainLayout;
    private int keyBoardStatus;

    private ImageView mTopLogoIv;

    private EditText mEtUserName;
    private TextView mEtusernameHint;
    private EditText mEtPassword;
    private TextView mEtPasswordHint;
    private LinearLayout ll_submit;
    private TextView mSubmitTv;

    private LinearLayout mRegistDescLl;
    private TextView mForgetPasswordTv;

    private TextView mRegistAgreementTv;
    private TextView mSwitchWayTv;
    private ImageView mRegistImgTv;

    private AlisaHandler alisaHandler;

    private boolean isClickSubmit = false;

    public static void startToActivity(Activity activity, boolean isLogin) {
        Bundle paramBundle = new Bundle();
        paramBundle.putBoolean("isLogin", isLogin);
        Util.xStartActivity(activity, LoginOrRegistActivity.class, paramBundle);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_or_regist);
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);

        if (AppData.getLoginStatus() != -1) {
            MainActivityGroup.startToActivity(this);
            finish();
            return;
        }
        Bundle bundle = getIntent().getExtras();
        if (null != bundle) {
            isLogin = bundle.getBoolean("isLogin");
        }
        bindViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(AppData.activityUrl.get())) {
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoginSubscription != null && !mLoginSubscription.isUnsubscribed()) {
            mLoginSubscription.unsubscribe();
        }


        if (mRegisterSubcription != null && !mRegisterSubcription.isUnsubscribed()) {
            mRegisterSubcription.unsubscribe();
        }

        if (alisaHandler != null) {
            alisaHandler.removeHandler();
        }
    }

    @Override
    protected void bindViews() {
        findViewById(R.id.lr_back).setOnClickListener(this);
        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mTopLogoIv = (ImageView) findViewById(R.id.logo);

        mEtUserName = (EditText) findViewById(R.id.etUserName);
        mEtUserName.addTextChangedListener(watcher);
        mEtUserName.setOnClickListener(this);
        mEtusernameHint = (TextView) findViewById(R.id.tv_username_hint);

        mEtPassword = (EditText) findViewById(R.id.etPassword);
        mEtPassword.addTextChangedListener(watcher);
        mEtPassword.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    mEtPassword.setText("");
                }
                return false;
            }
        });
        mEtPassword.setOnClickListener(this);
        mEtPasswordHint = (TextView) findViewById(R.id.tv_pwd_hint);
        mPasswordShowRl = (RelativeLayout) findViewById(R.id.rl_pwd_show);
        mPasswordShowRl.setOnClickListener(this);
        mPasswordShowIv = (ImageView) findViewById(R.id.iv_pwd_show);

        ll_submit = (LinearLayout) findViewById(R.id.ll_submit);
        ll_submit.setOnClickListener(this);
        mSubmitTv = (TextView) findViewById(R.id.tv_submit);

        mRegistDescLl = (LinearLayout) findViewById(R.id.ll_regist_desc);
        mRegistDescLl.setOnClickListener(this);
        mForgetPasswordTv = (TextView) findViewById(R.id.forgetPwd);
        Util.setUnderLine(mForgetPasswordTv);
        mForgetPasswordTv.setOnClickListener(this);
        mRegistAgreementTv = (TextView) findViewById(R.id.tv_regist_agreement);
        Util.setUnderLine(mRegistAgreementTv);
        mRegistAgreementTv.setOnClickListener(this);
        mSwitchWayTv = (TextView) findViewById(R.id.tv_other);
        mSwitchWayTv.setOnClickListener(this);
        mRegistImgTv = (ImageView) findViewById(R.id.iv_regist_action);
        mainLayout = (KeyboardLayout) findViewById(R.id.mainLayout);
        mainLinearLayout = (LinearLayout) findViewById(R.id.mainLinearLayout);
        mainLinearLayout.setLayoutParams(new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (com.iqianjin.client.utils.Util.getScreenHeight(this) * 0.8)));
        mTitleLayout = (LinearLayout) findViewById(R.id.titleLayout);
        // 初始化扩展动画
        mainLayout.setOnkbdStateListener(new KeyboardLayout.onKybdsChangeListener() {

            @Override
            public void onKeyBoardStateChange(int state) {
                switch (state) {
                    case KeyboardLayout.KEYBOARD_STATE_HIDE: // 隐藏软键盘
                        if (keyBoardStatus != state) {
                            keyBoardStatus = state;
                            mTopLogoIv.setVisibility(View.VISIBLE);
                            isClickSubmit = false;
                            startLayoutAnim(getResources().getDimensionPixelSize(R.dimen.titleKeyBoardShowHeight),
                                    getResources().getDimensionPixelSize(R.dimen.titleKeyBoardHideHeight), true);
                            startLayoutAnim(getResources().getDimensionPixelSize(R.dimen.titleKeyBoardShowHeight),
                                    getResources().getDimensionPixelSize(R.dimen.userInfoIconGreyWidth), false);
                        } else {

                        }
                        break;
                    case KeyboardLayout.KEYBOARD_STATE_SHOW: // 显示软键盘
                        if (keyBoardStatus != state) {
                            keyBoardStatus = state;
                            mTopLogoIv.setVisibility(View.GONE);
                            isClickSubmit = true;
                            startLayoutAnim(getResources().getDimensionPixelSize(R.dimen.titleKeyBoardHideHeight),
                                    getResources().getDimensionPixelSize(R.dimen.titleKeyBoardShowHeight), true);
                            startLayoutAnim(getResources().getDimensionPixelSize(R.dimen.userInfoIconGreyWidth),
                                    getResources().getDimensionPixelSize(R.dimen.titleKeyBoardShowHeight), false);
                        } else {

                        }
                        break;
                }

                mScrollView.scrollTo(0, -1000);
            }
        });
        initData();
    }

    protected void initData() {
        if (isLogin) {
            mEtusernameHint.setText("用户名/手机号/邮箱");
            mEtUserName.setInputType(InputType.TYPE_CLASS_TEXT);
            mEtUserName.setFilters(getLengthFilters(30));
            mEtPasswordHint.setText("登录密码");
            mRegistDescLl.setVisibility(View.GONE);
            mForgetPasswordTv.setVisibility(View.VISIBLE);
            mSubmitTv.setText("登录");
            mSwitchWayTv.setText("注册");
            mRegistImgTv.setVisibility(View.GONE);
            toVisibleAnim(mForgetPasswordTv, 600);
        } else {
            mEtusernameHint.setText("手机号");
            mEtUserName.setText("");
            mEtUserName.requestFocus();
            mEtUserName.setInputType(InputType.TYPE_CLASS_NUMBER);
            mEtUserName.setFilters(getLengthFilters(11));
            mEtPasswordHint.setText("6-20位不能全数字，不含特殊字符");
            mEtPassword.setText("");
            mRegistDescLl.setVisibility(View.VISIBLE);
            mForgetPasswordTv.setVisibility(View.GONE);
            mSubmitTv.setText("注册");
            mSwitchWayTv.setText("登录");
            mRegistImgTv.setVisibility(View.VISIBLE);
            getRedPocketIcon();
            toVisibleAnim(mRegistDescLl, 600);
        }
        toVisibleAnim(mSubmitTv, 500);
        toVisibleAnim(mSwitchWayTv, 500);
        mEtUserName.setTypeface(MyApplication.getInstance().getTypeface());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lr_back:
                if(isLogin){
                    AppStatisticsUtil.onEvent(mContext, "50011");
                }else{
                    AppStatisticsUtil.onEvent(mContext, "50002");
                }
                if (isFlag()) {
                    MainActivityGroup.startToActivity(this);
                } else {
                    backUpByRightOut();
                }
                break;
            case R.id.etUserName:
                if (isLogin) {
                    AppStatisticsUtil.onEvent(mContext, "40001");
                } else {
                    AppStatisticsUtil.onEvent(mContext, "40002");
                }
                break;
            case R.id.etPassword:
                if (isLogin) {
                    AppStatisticsUtil.onEvent(mContext, "40008");
                } else {
                    AppStatisticsUtil.onEvent(mContext, "40009");
                }
                break;
            case R.id.ll_submit: // 登录
                if (isLogin) {
                    AppStatisticsUtil.onEvent(mContext, "40004");
                } else {
                    AppStatisticsUtil.onEvent(mContext, "40012");
                }

                String username = Util.getEditTextString(mEtUserName);
                if (EtNullAnimation(mEtUserName, username, mEtusernameHint))
                    return;

                String password = Util.getEditTextString(mEtPassword);
                if (EtNullAnimation(mEtPassword, password, mEtPasswordHint))
                    return;

                if (isLogin) {
                    submitLogin();
                } else {
                    if (isUsernameValid(username))
                        return;

                    if (isPasswordValid(password)) {
                        showToast(this, "密码需6-20位");
                        mEtPassword.requestFocus();
                        return;
                    }
                    submitRegister();
                }
                break;
            case R.id.forgetPwd: // 忘记密码
                AppStatisticsUtil.onEvent(mContext, "40006");
                ForgetPasswordActivity.startToActivity(this);
                break;
            case R.id.tv_regist_agreement: // 查看注册协议
                AppStatisticsUtil.onEvent(mContext, "40011");
                IqianjinPublicModel model = new IqianjinPublicModel();
                model.setColumnType(H5Type.REGIST_AGREEMENT);
                model.setType(1);
                model.setTitle("注册协议");
                H5TransitionActivity.startToActivity(this, model);
                overridePendingTransition(R.anim.push_up, R.anim.noaction);
                break;
            case R.id.rl_pwd_show:
                if (!isShowPwd) {
                    if (isLogin) {
                        AppStatisticsUtil.onEvent(mContext, "40003");
                    } else {
                        AppStatisticsUtil.onEvent(mContext, "40010");
                    }
                    mEtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    mPasswordShowIv.setImageResource(R.drawable.icon_see);
                } else {
                    mEtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mPasswordShowIv.setImageResource(R.drawable.icon_secret);
                }
                mEtPassword.setTypeface(MyApplication.getInstance().getTypeface());
                mEtPassword.setSelection(mEtPassword.getText().toString().trim().length());
                isShowPwd = !isShowPwd;
                break;
            case R.id.tv_other:
                if (isLogin) {
                    AppStatisticsUtil.onEvent(mContext, "40007");
                }
                isLogin = !isLogin;
                initData();
                break;
            default:
                break;
        }
    }

    @NonNull
    private InputFilter[] getLengthFilters(int length) {
        return new InputFilter[]{new InputFilter.LengthFilter(length)};
    }

    private boolean isFlag() {
        return getIntent().getFlags() == Constants.TURNTOMAIN || getIntent().getIntExtra("flag", -1) == Constants.TURNTOMAIN;
    }

    public static boolean isUsernameValid(String userPhone) {
        if (userPhone.length() != 11) {
//            showToast(this, "手机号输入有误");
            return true;
        }
        return false;
    }

    protected boolean isPasswordValid(String password) {
        if (password.length() < 6 || password.length() > 20) {
            return true;
        }
        return false;
    }

    protected boolean EtNullAnimation(EditText editText, String editTextValue, TextView editTextHint) {
        if (TextUtils.isEmpty(editTextValue)) {
            editText.requestFocus();
            if (!isClickSubmit) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(editText, 0);
                isClickSubmit = true;
            } else {
                ViewShakeUtils.shake(mContext, editTextHint);
            }
            return true;
        }
        return false;
    }

    TextWatcher watcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            isEtHintVisible(mEtUserName, mEtusernameHint);
            isEtHintVisible(mEtPassword, mEtPasswordHint);
        }
    };

    private void isEtHintVisible(EditText editText, TextView editTextHint) {
        if (editText.getText().toString().trim().length() >= 1) {
            editTextHint.setVisibility(View.GONE);
        } else {
            editTextHint.setVisibility(View.VISIBLE);
        }
    }

    private Subscription mLoginSubscription;
    private Subscription mRegisterSubcription;

    private  void submitRegister(){
        showProgress(this);
        Observable<BaseResponse> registerObserver  = createProgress();
        mRegisterSubcription = registerObserver.lift(new MyOperator()).flatMap(new Func1<BaseResponse, Observable<BaseResponse>>() {
            @Override
            public Observable<BaseResponse> call(BaseResponse baseResponse) {
                return ObserverRegister();
            }
        }).subscribe(new Subscriber<BaseResponse>() {
            @Override
            public void onCompleted() {
                closeProgress();
            }

            @Override
            public void onError(Throwable e) {
                if (!mRegisterSubcription.isUnsubscribed()){
                    mRegisterSubcription.unsubscribe();
                }
                reportNetError(LoginOrRegistActivity.this);
                closeProgress();
            }

            @Override
            public void onNext(BaseResponse baseResponse) {
                if (!mRegisterSubcription.isUnsubscribed()){
                    mRegisterSubcription.unsubscribe();
                }
            }
        });
    }
    public Observable createProgress(){
        return  Observable.create(new Observable.OnSubscribe<BaseResponse>() {

            @Override
            public void call(Subscriber<? super BaseResponse> subscriber) {
                showProgress(LoginOrRegistActivity.this);
                subscriber.onNext(null);
            }
        });

    }

    /**
     * 登录
     */
    private void submitLogin() {
        Observable<BaseResponse> listObservable = createProgress();
        mLoginSubscription = listObservable.lift(new MyOperator()).flatMap(new Func1<BaseResponse, Observable<BaseResponse>>() {
            @Override
            public Observable<BaseResponse> call(BaseResponse baseResponse) {
                return login(Util.getEditTextString(mEtUserName), Util.getEditTextString(mEtPassword));
            }
        }).subscribe(new Subscriber<BaseResponse>() {
            @Override
            public void onCompleted() {
                closeProgress();
            }

            @Override
            public void onError(Throwable e) {
                if (!mLoginSubscription.isUnsubscribed()){
                    mLoginSubscription.unsubscribe();
                }

                reportNetError(LoginOrRegistActivity.this);
                closeProgress();
            }

            @Override
            public void onNext(BaseResponse baseResponse) {

            }
        });
    }



    private Observable login(final String username, final String password) {
        return Observable.create(new Observable.OnSubscribe<BaseResponse>() {
            @Override
            public void call(final Subscriber<? super BaseResponse> subscriber) {
                setPushAlisa();
                ReqParam reqParam = new ReqParam(LoginOrRegistActivity.this);
                reqParam.put("username", username);
                reqParam.put("passwd", getPwdByPublicKey(password));

                HttpClientUtils.post(LoginOrRegistActivity.this, ServerAddr.PATH_LOGIN, reqParam, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                        super.onSuccess(statusCode, headers, json);
                        if (subscriber.isUnsubscribed()){
                            return;
                        }
                        CommModelResponse response = new CommModelResponse(LoginOrRegistActivity.this);
                        response.parse(json, UserInfo.class);
                        closeProgress();
                        if (response.msgCode == 1) {
                            AppStatisticsUtil.onEvent(mContext, "40005");
                            UserInfo item = (UserInfo) response.item;
                            AppData.setUserId(item.getUserId());

                            UserInfoManager manager = new UserInfoManager(mContext);
                            UserInfo user = manager.getUserItem(AppData.getUserId());
                            if (null == user) {
                                user = new UserInfo();
                            }
                            user.setMobile(item.getMobile());
                            user.setUsername(item.getUsername());
                            user.setUserId(item.getUserId());
                            user.setIdVerified(item.getIdVerified());
                            user.setNewInvestor(item.getNewInvestor());
                            manager.saveUser(user);

                            AppData.userName.set(item.getUsername());
                            AppData.idVerified.set(item.getIdVerified());
                            ThreadUtil.sendMessage(Constants.LOGIN);
                            AppData.setLoginStatus(1);

                            if (!LockPatternUtils.getInstanceLockPattern(LoginOrRegistActivity.this).savedPatternExists()) {
                                Intent it = new Intent(LoginOrRegistActivity.this, GesturePasswordActivity.class);
                                Bundle b = new Bundle();
                                b.putInt("flag", getIntent().getIntExtra("flag", 0));
                                it.putExtras(b);
                                it.setFlags(getIntent().getFlags());
                                startActivity(it);
                            } else if (isFlag()) {
                                MainActivityGroup.startToActivity(LoginOrRegistActivity.this);
                            }
                            if (getIntent().getFlags() == Constants.TURNTOASSETS) {
                                ThreadUtil.sendMessage(Constants.TURNASSETS);
                            }

                            if (getIntent().getFlags() == Constants.TURNTOBACK_SSETRESULT) {
                                setResult(1000);
                            }

                            finish();
                            mEtPassword.setText("");
                            mEtUserName.setText("");

                            ThreadUtil.sendMessage(Constants.DELETENEWSAPP);
                        } else {
                            showToast(LoginOrRegistActivity.this, response.msgDesc);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        Observable.error(throwable);
                        super.onFailure(statusCode, headers, throwable, errorResponse);

                    }
                });
            }
        }).repeat(2);
    }
    private Observable ObserverRegister() {
        return  Observable.create(new Observable.OnSubscribe<BaseResponse>() {
            @Override
            public void call(Subscriber<? super BaseResponse> subscriber) {
                final String name = Util.getEditTextString(mEtUserName);
                final String password = Util.getEditTextString(mEtPassword);
                ReqParam reqParam = new ReqParam(LoginOrRegistActivity.this);
                reqParam.put("username", name);
                reqParam.put("passwd", getPwdByPublicKey(password));

                HttpClientUtils.post(LoginOrRegistActivity.this, ServerAddr.PATH_CONFIRM_USERNAME, reqParam, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                        super.onSuccess(statusCode, headers, json);
                        closeProgress();
                        BaseResponse response = new BaseResponse(LoginOrRegistActivity.this);
                        response.parse(json);
                        if (response.msgCode == 1) { // 请求成功
                            AppStatisticsUtil.onEvent(mContext, "40013");
                            mEtUserName.setText("");
                            mEtPassword.setText("");
                            mEtUserName.requestFocus();
                            RegistPhoneActivity.startToActivity(LoginOrRegistActivity.this, name, password);
                        } else {
                            showToast(LoginOrRegistActivity.this, response.msgDesc);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        super.onFailure(statusCode, headers, throwable, errorResponse);

                        Observable.error(throwable);
                    }
                });
            }
        });
    }

    class MyOperator implements Observable.Operator<BaseResponse,BaseResponse>{

        @Override
        public Subscriber<? super BaseResponse> call(final Subscriber<? super BaseResponse> subscriber) {
           return  new Subscriber<BaseResponse>() {
               @Override
               public void onCompleted() {

               }

               @Override
               public void onError(Throwable e) {
                   closeProgress();
               }

               @Override
               public void onNext(BaseResponse baseResponse) {
                    if (subscriber.isUnsubscribed()){
                        return;
                    }
                   if (!isHasPublicKey()){
                       rxPostPublicKey(subscriber);
                   }else{
                       subscriber.onNext(null);
                       subscriber.onCompleted();
                   }
               }
           };
        }

    }
    private void rxPostPublicKey(final Subscriber nextSubscriber){
        Observable.create(new Observable.OnSubscribe<BaseResponse>() {
            @Override
            public void call(final Subscriber<? super BaseResponse> subscriber) {
                ErrorUtils.getPublicKey(LoginOrRegistActivity.this,nextSubscriber);
            }
        }).subscribe(new Subscriber<BaseResponse>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                nextSubscriber.onError(e);
            }

            @Override
            public void onNext(BaseResponse baseResponse) {

            }
        });
    }





    /**
     * 是否有公钥
     *
     * @return
     */
    private boolean isHasPublicKey() {
        return null != AppData.publicKey.get() && AppData.publicKey.get().length() > 0;
    }

    /**
     * 将密码加密后返回
     *
     * @param password
     * @return
     */
    @Nullable
    private String getPwdByPublicKey(String password) {
        String pwdByPublicKey = "";
        try {
            pwdByPublicKey = RSAUtil.encryptByPublicKey(password, AppData.publicKey.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pwdByPublicKey;
    }


    /**
     * 第一次使用软件，获取公钥以后设置极光推送信息 1.注册极光推送 2.将手机信息注册到服务器：token = getIMEI
     */
    public void setPushAlisa() {
        XLog.i("re", "alisa  请求极光绑定");
        alisaHandler = new AlisaHandler(getApplicationContext());
        alisaHandler.postDelayed(alisaHandler.run, 1 * 1000);
    }

    private String restUrl;

    //注册前的红包图片地址
    private void getRedPocketIcon() {
        if (TextUtils.isEmpty(restUrl)) {
            ReqParam param = new ReqParam(this);
            HttpClientUtils.post(this, ServerAddr.PATH_RESET_IMG, param, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
                    super.onSuccess(statusCode, headers, json);
                    RegistImgResponse resp = new RegistImgResponse(LoginOrRegistActivity.this);
                    resp.parse(json);
                    if (resp.msgCode == 1 && !TextUtils.isEmpty(resp.imgUrl)) {
                        restUrl = resp.imgUrl;
                        setViewImage(mRegistImgTv, resp.imgUrl, NoRes.class);
                        toVisibleAnim(mRegistImgTv, 500);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    reportNetError(mContext);
                }
            });
        } else {
            setViewImage(mRegistImgTv, restUrl, NoRes.class);
            toVisibleAnim(mRegistImgTv, 500);
        }

    }


    private void startLayoutAnim(final int fromY, final int toY, final boolean isTop) {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(300);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (Float) animation.getAnimatedValue();
                ViewGroup.LayoutParams params;
                if (isTop) {
                    params = mTitleLayout.getLayoutParams();
                } else if (isLogin) {
                    params = mForgetPasswordTv.getLayoutParams();
                } else {
                    params = mRegistDescLl.getLayoutParams();
                }
                params.height = (int) (fromY + (toY - fromY) * currentValue);
                if (isTop) {
                    mTitleLayout.setLayoutParams(params);
                    mainLayout.requestLayout();
                } else {
                    mForgetPasswordTv.setLayoutParams(params);
                    mRegistDescLl.setLayoutParams(params);
                    params = mSwitchWayTv.getLayoutParams();
                    params.height = (int) (fromY + (toY - fromY) * currentValue);
                    mSwitchWayTv.setLayoutParams(params);
                }
            }
        });
        anim.start();
    }

    /**
     * 显示渐变动画
     *
     * @param view
     * @param time
     */
    private void toVisibleAnim(View view, int time) {
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).setDuration(time).start();
    }
}
