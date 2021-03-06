package com.homeworld.splitabill;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.ads.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

public class SplitABillActivity extends Activity {
	private AdView adView;
	/* Your ad unit id. Replace with your actual ad unit id. */
	private static final String AD_UNIT_ID = "ca-app-pub-1589512828092674/5202883744";

    EditText focusedEditText;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splitabill);

        final EditText total = (EditText) findViewById(R.id.total_value);
        setOnFocusChangeListener(total);

        final EditText serviceChargeValue = (EditText) findViewById(R.id.service_value);
        setOnFocusChangeListener(serviceChargeValue);

		SplitBill(0, new BigDecimal(0));

		// Initialize the Mobile Ads SDK.
		MobileAds.initialize(this, "ca-app-pub-1589512828092674~3726150548");

		// Create ad view
		adView = new AdView(this);
		adView.setAdUnitId(AD_UNIT_ID);
		adView.setAdSize(AdSize.SMART_BANNER);

		// Lookup your linearLayout
		LinearLayout layout = (LinearLayout) findViewById(R.id.ad_banner);

		// add adView
		layout.addView(adView);

		// initiate generic request
		AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
				.addTestDevice("A8805CEDE10091FF74A8E3CE08BCC426").build();

		// load add view with ad request
		adView.loadAd(adRequest);

		total.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				Currency currency = Currency.getInstance(Locale.getDefault());
				String digits = s.toString().replaceAll(
						String.format("[%s,.]", currency.getSymbol()), "");

				if (digits.length() <= 0) {
					return;
				}

				NumberFormat nf = NumberFormat.getCurrencyInstance();

				try {
					String formatted = nf.format(Double.parseDouble(digits) / 100);

					//trigger service charge calculation
					String scValue = serviceChargeValue.getText().toString();
					serviceChargeValue.setText(scValue); // update service charge value

					total.removeTextChangedListener(this);
					total.setText(formatted);
					total.setSelection(formatted.length());
					total.addTextChangedListener(this);

				} catch (NumberFormatException nfe) {
					// total.setText("");
				}
			}

			@Override
			public void afterTextChanged(Editable s) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
			}
		});

		((RadioGroup) findViewById(R.id.toggleGroup))
				.setOnCheckedChangeListener(ToggleListener);

		serviceChargeValue.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				ToggleButton btnPct = (ToggleButton) findViewById(R.id.btn_service_percent);
				boolean isPercent = btnPct.isChecked();
				TextView serviceChargeTotal = (TextView) findViewById(R.id.service_charge_total);

				Currency currency = Currency.getInstance(Locale.getDefault());
				String digits = s.toString().replaceAll(
						String.format("[%s,.]", currency.getSymbol()), "");
				digits = digits.replaceAll(String.format("[%s,.]", "%"), "");

				if (digits.length() <= 0) {
					return;
				}
				// format as monetary value
				NumberFormat nf = NumberFormat.getCurrencyInstance();
				if (isPercent) {
					try {
						Double billTotal = getBillTotal();
						Double serviceChargePercent = Double.parseDouble(digits);
						Double serviceChargeValue = (serviceChargePercent / 100) * billTotal; 
						
						String formatted = nf.format(serviceChargeValue);
						serviceChargeTotal.setText(formatted);
						setGrandTotal(currency, serviceChargeValue.toString(), nf);
					} catch (NumberFormatException nfe) {
						// total.setText("");
					}
				} else {
					// format value as percent
					Double billTotal = getBillTotal();
					Double serviceChargeValue = Double.parseDouble(digits);
					Double serviceChargePercent;
					
					if(billTotal == null || billTotal == 0d)
					{
						serviceChargePercent = 0d;
					}
					else
					{
						serviceChargePercent = (serviceChargeValue / billTotal) * 100;
					}
					
					String pctFormated = String.format(Locale.ENGLISH, "%.2f %%", serviceChargePercent);

					serviceChargeTotal.setText(pctFormated);					
					setGrandTotal(currency, digits, nf);
				}
			}

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}
		});

		serviceChargeValue.setText("20"); // set default service charge value
        total.requestFocus();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	static final RadioGroup.OnCheckedChangeListener ToggleListener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(final RadioGroup radioGroup, final int i) {
			for (int j = 0; j < radioGroup.getChildCount(); j++) {
				final ToggleButton view = (ToggleButton) radioGroup
						.getChildAt(j);
				view.setChecked(view.getId() == i);
			}
		}
	};

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		int splitValue = savedInstanceState.getInt("splitBy");
		TextView splitBy = (TextView) findViewById(R.id.edit_splitby);
		splitBy.setText(String.format(Locale.ENGLISH,"%02d", splitValue));

		TextView grandTotal = (TextView) findViewById(R.id.subtotal_value);
		String total = grandTotal.getText().toString();
		if (!total.isEmpty()) {
			BigDecimal totalValue = new BigDecimal(CurrencyStringClean(total));
			if (totalValue.signum() > 0)
				SplitBill(splitValue, totalValue);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		TextView splitBy = (TextView) findViewById(R.id.edit_splitby);
		int split = Integer.parseInt(splitBy.getText().toString());
		outState.putInt("splitBy", split);
	}

	@Override
	protected void onDestroy() {
		if(adView != null){
			adView.destroy();
		}

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		if(adView != null) {
			adView.pause();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(adView != null) {
			adView.resume();
		}
	}

	public void splitMinus(View view) {
		try {
			TextView splitBy = (TextView) findViewById(R.id.edit_splitby);
			int split = Integer.parseInt(splitBy.getText().toString());

			if (split > 0) {
				split--;
			}

			splitBy.setText(String.format(Locale.ENGLISH,"%02d", split));

			TextView grandTotal = (TextView) findViewById(R.id.subtotal_value);
			BigDecimal totalValue = new BigDecimal(
					CurrencyStringClean(grandTotal.getText().toString()));
			if (totalValue.signum() > 0)
				SplitBill(split, totalValue);
		} catch (NumberFormatException nfe) {
			System.out.println("Could not parse " + nfe);
		}
	}

	public void splitPlus(View view) {
		try {
			TextView splitBy = (TextView) findViewById(R.id.edit_splitby);
			int split = Integer.parseInt(splitBy.getText().toString());

			if (split < 99) {
				split++;
			}

			splitBy.setText(String.format(Locale.ENGLISH,"%02d", split));

			TextView grandTotal = (TextView) findViewById(R.id.subtotal_value);
			BigDecimal totalValue = new BigDecimal(
					CurrencyStringClean(grandTotal.getText().toString()));
			if (totalValue.signum() > 0)
				SplitBill(split, totalValue);
		} catch (NumberFormatException nfe) {
			System.out.println("Could not parse " + nfe);
		}
	}

	public void onServiceTypeChange(View view) {
		((RadioGroup) view.getParent()).check(0);
		((RadioGroup) view.getParent()).check(view.getId());
		EditText serviceChargeValue = (EditText) findViewById(R.id.service_value);
		String scValue = serviceChargeValue.getText().toString();
        serviceChargeValue.setText(scValue); // update service charge value
	}
	
	private Double getBillTotal() {
		Currency currency = Currency.getInstance(Locale.getDefault());
		final EditText total = (EditText) findViewById(R.id.total_value);
		
		String sTotal = total.getText().toString().replaceAll(
				String.format("[%s,.]", currency.getSymbol()), "");
		sTotal = sTotal.replaceAll(String.format("[%s,.]", "%"), "");
		
		if (sTotal.length() <= 0) {
			return 0d;
		}
		else
		{
            return Double.parseDouble(sTotal) / 100;
		}
	}

	private void setGrandTotal(Currency currency, String serviceCharge,	NumberFormat nf) {
		//set grand total
		final EditText billTotal = (EditText) findViewById(R.id.total_value);
		
		String sTotal = billTotal.getText().toString().replaceAll(
				String.format("[%s,.]", currency.getSymbol()), "");
		sTotal = sTotal.replaceAll(String.format("[%s,.]", "%"), "");
		
		if (sTotal.length() <= 0) {
			return;
		}
		
		Double totalValue = Double.parseDouble(sTotal) / 100;
		Double grandTotal = totalValue + Double.parseDouble(serviceCharge);
		
		TextView grandTotalValue = (TextView) findViewById(R.id.subtotal_value);
		
		try {
			String formatted = nf.format(grandTotal);
			grandTotalValue.setText(formatted);

            BigDecimal subTotalValue = new BigDecimal(CurrencyStringClean(grandTotalValue.getText().toString()));
            if (subTotalValue.signum() > 0)
            {
                TextView splitBy = (TextView) findViewById(R.id.edit_splitby);
                int split = Integer.parseInt(splitBy.getText().toString());
                SplitBill(split, subTotalValue);
            }
		} catch (NumberFormatException nfe) {
			// total.setText("");
		}
	}

    private void setOnFocusChangeListener(TextView textView){

        textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    focusedEditText = (EditText)v;
                    //focusedEditText.setSelection(focusedEditText.getText().length());
                }
            }
        });
    }

	private String CurrencyStringClean(String formatted) {
		if (formatted == null || formatted.isEmpty())
			return "";

		Currency currency = Currency.getInstance(Locale.getDefault());
		String digits = formatted.replaceAll(
				String.format("[%s,.]", currency.getSymbol()), "");
		Double num = Double.parseDouble(digits) / 100;
		return num.toString();
	}

	private void SplitBill(int split, BigDecimal billTotal) {
		final SplitBillAdapter adapter;
		final ListView listview = (ListView) findViewById(R.id.listview_bills);

		ArrayList<BillItem> billList = new ArrayList<>();

		if (split <= 0 || billTotal.signum() < 1) {
			adapter = new SplitBillAdapter(this, billList);
			listview.setAdapter(adapter);

            if(focusedEditText != null) {
                focusedEditText.requestFocus();
            }
			return;
		}

		Money money = MoneyMaker.makeMoney(
				Currency.getInstance(Locale.getDefault()), billTotal);

		Money[] bills = money.proRate(split);

		int index = 0;
		for (Money bill : bills) {
			index++;
			billList.add(new BillItem(String.format(Locale.ENGLISH,"Bill %01d", index),
					NumberFormat.getCurrencyInstance().format(bill.value())));
		}

		adapter = new SplitBillAdapter(this, billList);

		listview.setAdapter(adapter);
        if(focusedEditText != null) {
            focusedEditText.requestFocus();
        }
	}
}
