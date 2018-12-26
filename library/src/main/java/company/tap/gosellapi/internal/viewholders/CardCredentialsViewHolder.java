package company.tap.gosellapi.internal.viewholders;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import company.tap.gosellapi.R;
import company.tap.gosellapi.internal.adapters.CardSystemsRecyclerViewAdapter;
import company.tap.gosellapi.internal.api.models.PaymentOption;
import company.tap.gosellapi.internal.custom_views.CvvEditText;
import company.tap.gosellapi.internal.custom_views.ExpirationDateEditText;
import company.tap.gosellapi.internal.data_managers.PaymentDataManager;
import company.tap.gosellapi.internal.data_managers.payment_options.view_models.CardCredentialsViewModel;
import company.tap.gosellapi.internal.data_managers.payment_options.view_models_data.CardCredentialsViewModelData;
import company.tap.gosellapi.internal.utils.CardType;
import company.tap.tapcardvalidator_android.CardBrand;
import company.tap.tapcardvalidator_android.CardValidationState;
import company.tap.tapcardvalidator_android.CardValidator;
import company.tap.tapcardvalidator_android.DefinedCardBrand;

public class CardCredentialsViewHolder
        extends PaymentOptionsBaseViewHolder<CardCredentialsViewModelData, CardCredentialsViewHolder, CardCredentialsViewModel> {

    public interface Data {

        void bindCardNumberFieldWithWatcher(EditText cardNumberField);

        SpannableString getCardNumberText();
        String getExpirationDateText();
        String getCVVText();
        String getCardholderNameText();
    }

    private final static int BIN_NUMBER_LENGTH = 6;
    private final static int NAME_ON_CARD_MAX_LENGTH = 26;
    private EditText cardNumberField;
    private ImageButton cardScannerButton;
    private ExpirationDateEditText expirationDateField;
    private CvvEditText cvvField;
    private EditText nameOnCardField;
    private TextInputLayout addressOnCardLayout;
    private EditText addressOnCardField;
    private ConstraintLayout saveCardLayout;
    private TextView saveCardDescriptionTextView;
    private Switch saveCardSwitch;
    private RecyclerView cardSystemsRecyclerView;
    private CardCredentialsViewModel viewModel;


    CardCredentialsViewHolder(View view) {

        super(view);

/////////////////////////////////////////////////// CARD NUMBER START ///////////////////////////////////////////////////////
        cardNumberField = itemView.findViewById(R.id.cardNumberField);

        cardNumberField.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str =s.toString();

                DefinedCardBrand brand = validateCardNumber(str);
                // String str = validateLength(s.toString());
                CardBrand cardBrand = brand.getCardBrand();

                int[] spacings;
                if (cardBrand != CardBrand.americanExpress) {
                    spacings = new int[]{4, 8, 12};
                } else {
                    spacings = new int[]{3, 9};
                }

                String text = str.toString();
                text = text.replace(" ", "");
                SpannableStringBuilder cardNumber = new SpannableStringBuilder(text);

                for (int i = spacings.length - 1; i >= 0; i--) {

                    int space = spacings[i];

                    if (space < text.length()) {
                        cardNumber.insert(space, " ");
                    }
                }

                cardNumberField.removeTextChangedListener(this);
                cardNumberField.setText(cardNumber.toString());

                int selectionIndex = start + count;

                for (int i = 0; i < spacings.length; i++) {

                    int space = spacings[i];

                    int incrementedSpace = space + i;

                    if (start <= incrementedSpace && incrementedSpace < start + count) {
                        selectionIndex++;
                    }

                    if (start - 1 == incrementedSpace && count == 0) {
                        selectionIndex--;
                    }
                }

                try {
                    cardNumberField.setSelection(selectionIndex);
                } catch (Exception e) {
                    cardNumberField.setSelection(cardNumber.length());
                }

                cardNumberField.addTextChangedListener(this);
            }

            private String validateLength(String cardNumber) {

                cardNumber = cardNumber.replace(" ", "");
                System.out.println("brand >>  card number : replaced card no = " + cardNumber);
                DefinedCardBrand brand = CardValidator.validate(cardNumber);
                System.out.println("brand >>  card number :" + cardNumber + " brand:"+brand.getCardBrand());

                String str = cardNumberField.getText().toString();
                String newStr = str;
                if (brand.getValidationState().equals(CardValidationState.invalid)) {
                    newStr =  str.substring(0,str.length()-1);
                }
                return newStr;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////// CARD SCANNER START ///////////////////////////////////////////////////////
        cardScannerButton = itemView.findViewById(R.id.cardScannerButton);
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////// CARD EXPIRATION_DATE START ///////////////////////////////////////////////////////
        expirationDateField = itemView.findViewById(R.id.expirationDateField);
        // enable Expiration date dialog
        expirationDateField.useDialogForExpirationDateEntry((Activity) view.getContext(), true);
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////// CVV START ///////////////////////////////////////////////////////
        cvvField = itemView.findViewById(R.id.cvvField);
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////// NAME ON CARD START ///////////////////////////////////////////////////////
        nameOnCardField = itemView.findViewById(R.id.cardholderNameField);

        // Name on card
        nameOnCardField = itemView.findViewById(R.id.cardholderNameField);

        nameOnCardField.setFilters(new InputFilter[] {new InputFilter.AllCaps(), new InputFilter.LengthFilter(NAME_ON_CARD_MAX_LENGTH),
            new InputFilter() {

                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                    boolean keepOriginal = true;

                    StringBuilder sb = new StringBuilder(end - start);

                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);

                        if (isCharAllowed(c)) // put your condition here
                            sb.append(c);
                        else
                            keepOriginal = false;
                    }

                    if (keepOriginal)
                        return null;
                    else {
                        if (source instanceof Spanned) {
                            SpannableString sp = new SpannableString(sb);
                            TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
                            return sp;
                        } else {
                            return sb;
                        }
                    }
                }

                private boolean isCharAllowed(char c) {
                    return Character.isLetterOrDigit(c) || Character.isSpaceChar(c);
                }

            } });
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////// ADDRESS ON CARD START ///////////////////////////////////////////////////////
        addressOnCardLayout = itemView.findViewById(R.id.addressOnCardContainer);
        addressOnCardField = itemView.findViewById(R.id.addressOnCardTextView);
        addressOnCardField.setText("Address will be displayed here, probably in multiple lines.");
        setupAddressOnCardField();
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////// SAVE CARD START ///////////////////////////////////////////////////////
        saveCardLayout = itemView.findViewById(R.id.saveCardContainer);
        saveCardDescriptionTextView = itemView.findViewById(R.id.saveCardDescriptionTextView);
        saveCardSwitch = itemView.findViewById(R.id.saveCardSwitch);
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////// SETUP CARD PAYMENT OPTIONS START ///////////////////////////////////////////////////////
        initCardSystemsRecyclerView(getPaymentOption());
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}


    private ArrayList<PaymentOption> getPaymentOption() {
        return PaymentDataManager.getInstance().getPaymentOptionsDataManager().getPaymentOptionsResponse().getPaymentOptions();
    }

    @Override
    public void bind(CardCredentialsViewModelData data) {

     if(viewModel!=null)   viewModel.bindCardNumberFieldWithWatcher(cardNumberField);

//        cardScannerButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                viewModel.cardScannerButtonClicked();
//            }
//        });
//
//        addressOnCardLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                viewModel.addressOnCardClicked();
//            }
//        });
//
//        saveCardSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                viewModel.saveCardSwitchClicked(isChecked);
//            }
//        });
//
//        if (!viewModel.getCardNumber().isEmpty()) {
//            cardNumberField.setText(viewModel.getCardNumber());
//        }
//
//        if (!viewModel.getExpirationMonth().isEmpty() && !viewModel.getExpirationYear().isEmpty()) {
//            String expirationDate = viewModel.getExpirationMonth() + "/" + String.format("%02d", Integer.valueOf(viewModel.getExpirationYear()) % 100);
//            expirationDateField.setText(expirationDate);
//        }
//
//        if (viewModel.isShowAddressOnCardCell()) {
//            addressOnCardLayout.setVisibility(View.VISIBLE);
//        } else {
//            addressOnCardLayout.setVisibility(View.GONE);
//        }
////        if (!viewModel.getCVVnumber().isEmpty()) {
////            CVVField.setText(viewModel.getCVVnumber());
////        }
//
////        if (!viewModel.getNameOnCard().isEmpty()) {
////            nameOnCardField.setText(viewModel.getNameOnCard());
////        }
//
//        RelativeLayout saveCardSwitchContainer = itemView.findViewById(R.id.saveCardSwitchContainer);
//        saveCardSwitchContainer.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        viewModel.setCardSwitchHeight(saveCardSwitchContainer.getMeasuredHeight());
//
//        initCardSystemsRecyclerView(data.getPaymentOptions());
    }

    @Override
    public void setFocused(boolean isFocused) {
        itemView.setSelected(isFocused);
    }

    private void initCardSystemsRecyclerView(ArrayList<PaymentOption> paymentOptions) {

        cardSystemsRecyclerView = itemView.findViewById(R.id.cardSystemsRecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.VERTICAL, false);
        cardSystemsRecyclerView.setLayoutManager(linearLayoutManager);

        CardSystemsRecyclerViewAdapter adapter = new CardSystemsRecyclerViewAdapter(paymentOptions);
        cardSystemsRecyclerView.setAdapter(adapter);
    }

    private void updateCardSystemsRecyclerView(CardBrand brand) {
        RecyclerView cardSystemsRecyclerView = itemView.findViewById(R.id.cardSystemsRecyclerView);
        cardSystemsRecyclerView.invalidate();
        CardSystemsRecyclerViewAdapter adapter = (CardSystemsRecyclerViewAdapter) cardSystemsRecyclerView.getAdapter();
        adapter.updateForCardBrand(brand);
    }

    public void updateAddressOnCardView(boolean isShow) {
        if (isShow) {
            addressOnCardLayout.setVisibility(View.VISIBLE);
        } else {
            addressOnCardLayout.setVisibility(View.GONE);
        }
    }

    private DefinedCardBrand validateCardNumber(String cardNumber) {

        cardNumber = cardNumber.replace(" ", "");

        DefinedCardBrand brand = CardValidator.validate(cardNumber);
        System.out.println("brand >>  card number :" + cardNumber + " brand:"+brand.getCardBrand());

        // update CCVEditText CardType: to set CCV Length according to CardType
        updateCCVEditTextCardType(brand.getCardBrand());
        updateCardSystemsRecyclerView(brand.getCardBrand());

        if (brand.getValidationState().equals(CardValidationState.invalid)) {
            cardNumberField.setTextColor(itemView.getResources().getColor(R.color.red));
        } else {
            cardNumberField.setTextColor(itemView.getResources().getColor(R.color.greyish_brown));
        }
        return brand;
    }


    /**
     * Update CVV card type to identify CVV length
     * @param cardBrand
     */
    private void updateCCVEditTextCardType(CardBrand cardBrand){
      System.out.println("updateCCVEditTextCardType : " + cardBrand);
      if (cardBrand == null) return;

      System.out.println("updateCCVEditTextCardType : " + cardBrand.getRawValue());

      if (cardBrand.getRawValue().contains("AMEX") || cardBrand.getRawValue().contains("AMERICAN_EXPRESS"))
        cvvField.setCardType(CardType.AMEX);

        if (cardBrand.getRawValue().contains("VISA"))
            cvvField.setCardType(CardType.VISA);

        if (cardBrand.getRawValue().contains("DISCOVER"))
            cvvField.setCardType(CardType.DISCOVER);

        if (cardBrand.getRawValue().contains("DINERS_CLUB") || cardBrand.getRawValue().contains("DINERS"))
            cvvField.setCardType(CardType.DINERS_CLUB);

        if (cardBrand.getRawValue().contains("MADA"))
            cvvField.setCardType(CardType.MADA);

        if (cardBrand.getRawValue().contains("MAESTRO"))
            cvvField.setCardType(CardType.MAESTRO);

        if (cardBrand.getRawValue().contains("MASTERCARD"))
            cvvField.setCardType(CardType.MASTERCARD);

        if (cardBrand.getRawValue().contains("UNION_PAY") || cardBrand.getRawValue().contains("UNIONPAY"))
            cvvField.setCardType(CardType.UNIONPAY);

//      if (cardBrand.getRawValue().contains("CARDGUARD"))
//        cvvField.setCardType(CardType.CARDGUARD);

//      if (cardBrand.getRawValue().contains("CBK"))
//        cvvField.setCardType(CardType.CBK);

//      if (cardBrand.getRawValue().contains("DANKORT"))
//        cvvField.setCardType(CardType.DANKORT);

//      if (cardBrand.getRawValue().contains("FAWRY"))
//        cvvField.setCardType(CardType.AMEX);

//      if (cardBrand.getRawValue().contains("INSTAPAY"))
//        cvvField.setCardType(CardType.AMEX);

//      if (cardBrand.getRawValue().contains("INTERPAY"))
//        cvvField.setCardType(CardType.AMEX);

//      if (cardBrand.getRawValue().contains("JCB"))
//        cvvField.setCardType(CardType.AMEX);

//      if (cardBrand.getRawValue().contains("NAPS"))
//        cvvField.setCardType(CardType.AMEX);

//
//      if (cardBrand.getRawValue().contains("NSPK"))
//        cvvField.setCardType(CardType.AMEX);
//
//      if (cardBrand.getRawValue().contains("OMAN_NET"))
//        cvvField.setCardType(CardType.AMEX);
//
//      if (cardBrand.getRawValue().contains("SADAD_ACCOUNT"))
//        cvvField.setCardType(CardType.AMEX);
//
//      if (cardBrand.getRawValue().contains("TAP"))
//        cvvField.setCardType(CardType.AMEX);
//
//      if (cardBrand.getRawValue().contains("UATP"))
//        cvvField.setCardType(CardType.AMEX);

//      if (cardBrand.getRawValue().contains("VERVE"))
//        cvvField.setCardType(CardType.AMEX);

//
//      if (cardBrand.getRawValue().contains("Viva PAY"))
//        cvvField.setCardType(CardType.AMEX);
//
//      if (cardBrand.getRawValue().contains("Wataniya PAY"))
//        cvvField.setCardType(CardType.AMEX);
//
//      if (cardBrand.getRawValue().contains("Zain PAY"))
//        cvvField.setCardType(CardType.AMEX);
    }

    private void setupAddressOnCardField() {

        getAddressOnCardField().addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {

                int topPadding      = s.length() > 0 ? 16 : 0;
                float scale         = itemView.getResources().getDisplayMetrics().density;
                topPadding          = (int) (topPadding * scale + 0.5);

                int leftPadding     = getAddressOnCardField().getPaddingLeft();
                int rightPadding    = getAddressOnCardField().getPaddingRight();
                int bottomPadding   = getAddressOnCardField().getPaddingBottom();

                getAddressOnCardField().setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
            }
        });
    }

    private static class TrackingSpan extends ReplacementSpan {

        private float mTrackingPx;

        public TrackingSpan(float tracking) {
            mTrackingPx = tracking;
        }

        @Override
        public int getSize(Paint paint, CharSequence text,
                           int start, int end, Paint.FontMetricsInt fm) {
            return (int) (paint.measureText(text, start, end)
                    + mTrackingPx * (end - start - 1));
        }

        @Override
        public void draw(Canvas canvas, CharSequence text,
                         int start, int end, float x, int top, int y,
                         int bottom, Paint paint) {
            float dx = x;
            for (int i = start; i < end; i++) {
                canvas.drawText(text, i, i + 1, dx, y, paint);
                dx += paint.measureText(text, i, i + 1) + mTrackingPx;
            }
        }
    }

    private EditText getAddressOnCardField() {

        return addressOnCardField;
    }
}