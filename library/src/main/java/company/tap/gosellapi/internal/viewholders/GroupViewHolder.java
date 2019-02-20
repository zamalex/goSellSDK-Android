package company.tap.gosellapi.internal.viewholders;

import android.view.View;
import android.widget.TextView;

import company.tap.gosellapi.R;
import company.tap.gosellapi.internal.data_managers.payment_options.view_models.GroupViewModel;

/**
 * The type Group view holder.
 */
public class GroupViewHolder extends PaymentOptionsBaseViewHolder<String, GroupViewHolder, GroupViewModel> {

    /**
     * Instantiates a new Group view holder.
     *
     * @param view the view
     */
    public GroupViewHolder(View view) {

        super(view);
        this.titleTextView = view.findViewById(R.id.titleTextView);
    }

    @Override
    public void bind(String data) {

        titleTextView.setText(data);
    }

    private TextView titleTextView;
}
