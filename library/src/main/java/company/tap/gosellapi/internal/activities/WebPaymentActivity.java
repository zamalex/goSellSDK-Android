package company.tap.gosellapi.internal.activities;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.Random;

import company.tap.gosellapi.R;
import company.tap.gosellapi.internal.api.callbacks.GoSellError;
import company.tap.gosellapi.internal.api.enums.AuthenticationStatus;
import company.tap.gosellapi.internal.api.enums.ChargeStatus;
import company.tap.gosellapi.internal.api.models.Authenticate;
import company.tap.gosellapi.internal.api.models.Authorize;
import company.tap.gosellapi.internal.api.models.Charge;
import company.tap.gosellapi.internal.api.models.PaymentOption;
import company.tap.gosellapi.internal.data_managers.PaymentDataManager;
import company.tap.gosellapi.internal.data_managers.LoadingScreenManager;
import company.tap.gosellapi.internal.data_managers.payment_options.view_models.WebPaymentViewModel;
import company.tap.gosellapi.internal.interfaces.IPaymentProcessListener;
import company.tap.gosellapi.internal.utils.ActivityDataExchanger;


public class WebPaymentActivity extends BaseActionBarActivity implements IPaymentProcessListener {
  private WebPaymentViewModel viewModel;
  private WebView webView;
  String paymentURL;
  String returnURL;
  private Charge chargeOrAuthorize;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    /**
     * ActivityDataExchanger old way Replaced by Haitham >> I created a new way of getting PaymentOptionModel from ActivityDataExchanger
     */
//        viewModel = (WebPaymentViewModel) ActivityDataExchanger.getInstance().getExtra(getIntent(), IntentParameters.paymentOptionModel);
    Object viewModelObject = null;
    if (ActivityDataExchanger.getInstance().getWebPaymentViewModel() != null) {
      viewModelObject = ActivityDataExchanger.getInstance().getWebPaymentViewModel();
    }

    viewModel = (viewModelObject instanceof WebPaymentViewModel) ? (WebPaymentViewModel) viewModelObject : null;
    System.out.println(" WebPaymentActivity >>  viewModel :" + viewModel);
    setContentView(R.layout.gosellapi_activity_web_payment);

    webView = findViewById(R.id.webPaymentWebView);
    webView.setWebViewClient(new WebPaymentWebViewClient());
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);

    setTitle(getPaymentOption().getName());
    setImage(getPaymentOption().getImage());
    //Get the view which you added by activity setContentView() method
    View view = getWindow().getDecorView().findViewById(android.R.id.content);
    /**
     * post causes the Runnable to be added to the message queue
     * Runnable : used to run code in a different Thread
     * run () : Starts executing the active part of the class' code.
     *  >>> Here we try to get data in another thread not the  main thread to avoid UI Freezing
     */
    view.post(new Runnable() {
      @Override
      public void run() {
        getData();
      }
    });
  }

  private void getData() {

    LoadingScreenManager.getInstance().showLoadingScreen(this);
    PaymentDataManager.getInstance().initiatePayment(viewModel, this);
  }

  private void updateWebView() {

    WebView webView = findViewById(R.id.webPaymentWebView);
    webView.setVisibility(View.VISIBLE);

    if (paymentURL == null) return;

    webView.loadUrl(paymentURL);
  }

  private void finishActivityWithResultCodeOK() {
    setResult(RESULT_OK);
    finish();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    return super.onCreateOptionsMenu(menu);
  }

  /**
   * Listen to webview events
   */
  private class WebPaymentWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      System.out.println(" shouldOverrideUrlLoading : " + url);
      PaymentDataManager.WebPaymentURLDecision decision = PaymentDataManager.getInstance()
          .decisionForWebPaymentURL(url);

      boolean shouldOverride = !decision.shouldLoad();
      System.out.println(" shouldOverrideUrlLoading : decision : " + shouldOverride);
      if (shouldOverride) { // if decision is true and response has TAP_ID
        // call backend to get charge response >> based of charge object type [Authorize - Charge] call retrieveCharge / retrieveAuthorize
        PaymentDataManager.getInstance().retrieveChargeOrAuthorizeAPI(getChargeOrAuthorize());
      }
      return shouldOverride;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      System.out.println(" webpayment webview finished loading URL :" + url);
      super.onPageFinished(view, url);
      LoadingScreenManager.getInstance().closeLoadingScreen();
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
      super.onReceivedError(view, request, error);

      view.stopLoading();
      view.loadUrl("about:blank");
    }
  }

  @Override
  public void onBackPressed() {
    setResult(RESULT_CANCELED);
    super.onBackPressed();
  }

  @Override
  public void didReceiveCharge(Charge charge) {
    System.out.println("* * * " + charge.getStatus());
    if (charge != null) {
      switch (charge.getStatus()) {
        case INITIATED:

          break;
        case CAPTURED: AUTHORIZED:
          paymentSuccess();


          break;

      }
    }
    obtainPaymentURLFromChargeOrAuthorize(charge);
  }

  public static Intent getDismissIntent(int notificationId, Context context) {
    Intent intent = new Intent(context, GoSellPaymentActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    intent.putExtra("NOTIFICATION_ID", notificationId);
    //PendingIntent dismissIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    return intent;
  }

  private void paymentSuccess() {
    // show success bar
    DisplayMetrics displayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    int height = displayMetrics.heightPixels;
    int width = displayMetrics.widthPixels;
    PopupWindow popupWindow;
    try {
// We need to get the instance of the LayoutInflater
      LayoutInflater inflater = (LayoutInflater) this
          .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View layout = inflater.inflate(R.layout.charge_status_layout,
          (ViewGroup) findViewById(R.id.popup_element));


      popupWindow = new PopupWindow(layout, width, 150, true);

      ImageView closeIcon = layout.findViewById(R.id.close_icon);
      TextView statusText = layout.findViewById(R.id.status_text);
      TextView chargeText = layout.findViewById(R.id.charge_id_txt);

      statusText.setText(R.string.payment_status_alert_successful);
      chargeText.setText(getChargeOrAuthorize().getId());

      closeIcon.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {

          closePaymentActivity();
          popupWindow.dismiss();
          finish();
        }
      });
      popupWindow.showAtLocation(layout, Gravity.TOP, 0, 50);
      popupWindow.setAnimationStyle(R.style.popup_window_animation_phone);


    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void closePaymentActivity(){
    ActivityDataExchanger activityDataExchanger = ActivityDataExchanger.getInstance();
    System.out.println("activityDataExchanger.getClientActivity() >> " + activityDataExchanger.getClientActivity());
    Intent intent = new Intent(this,activityDataExchanger.getClientActivity());
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.putExtra("charge_id",getChargeOrAuthorize().getId());
    startActivity(intent);

  }

  @Override
  public void didReceiveAuthorize(Authorize authorize) {

    obtainPaymentURLFromChargeOrAuthorize(authorize);
  }

  @Override
  public void didReceiveError(GoSellError error) {

    closeLoadingScreen();
  }

  private void obtainPaymentURLFromChargeOrAuthorize(Charge chargeOrAuthorize) {
    System.out
        .println(" WebPaymentActivity >> chargeOrAuthorize : " + chargeOrAuthorize.getStatus());

    if (chargeOrAuthorize.getStatus() != ChargeStatus.INITIATED) {
      return;
    }

    Authenticate authentication = chargeOrAuthorize.getAuthenticate();
    if (authentication != null)
      System.out.println(" WebPaymentActivity >> authentication : " + authentication.getStatus());
    if (authentication != null && authentication.getStatus() == AuthenticationStatus.INITIATED) {
      return;
    }

    String url = chargeOrAuthorize.getTransaction().getUrl();
    System.out.println("WebPaymentActivity >> Transaction().getUrl() :" + url);
    System.out.println("WebPaymentActivity >> chargeOrAuthorize :" + chargeOrAuthorize.getId());


    if (url != null) {
      // save charge id
      setChargeOrAuthorize(chargeOrAuthorize);
      this.paymentURL = url;
      updateWebView();
    }
  }

  private void setChargeOrAuthorize(Charge chargeOrAuthorize) {
    this.chargeOrAuthorize = chargeOrAuthorize;
  }

  private Charge getChargeOrAuthorize() {
    return this.chargeOrAuthorize;
  }

  private void closeLoadingScreen() {

    LoadingScreenManager.getInstance().closeLoadingScreen();
  }

  private PaymentOption getPaymentOption() {

    return viewModel.getData();
  }

  public final class IntentParameters {

    public static final String paymentOptionModel = "payment_option_model";
  }
}
