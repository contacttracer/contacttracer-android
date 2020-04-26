package com.dawsoftware.contacttracker.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.util.DrawableUtil;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class CustomScaleView extends View {
	
	private static final int MIN_SIDE = 20;
	private static final int INTERNAL_PADDING = 0;
	
	private Drawable icon;
	private Drawable[] allDrawables;
	
	@ColorInt
	private int tintColor;
	@ColorInt
	private int defaultTintColor;
	
	@ColorInt
	private int defaultColor;
	
	private int elementsCount;
	
	private int tintedElementsCount;
	
	private int  iconSideSize = MIN_SIDE;
	
	private int internalPadding = INTERNAL_PADDING;
	
	public CustomScaleView(final Context context) {
		super(context);
		init(null);
	}
	
	public CustomScaleView(final Context context, @Nullable final AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public CustomScaleView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	public CustomScaleView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(attrs);
	}
	
	private void init(@Nullable final AttributeSet attrs) {
		defaultColor = getContext().getResources().getColor(R.color.white);
		defaultTintColor = getContext().getResources().getColor(R.color.sick_red);
		
		parseAttrs(attrs);
		
		allDrawables = new Drawable[elementsCount];
		for (int i = 0; i < elementsCount; i++) {
			allDrawables[i] = icon.getConstantState().newDrawable().mutate();
		}
	}
	
	private void parseAttrs(@Nullable final AttributeSet attrs) {
		if (attrs != null) {
			final TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CustomScaleView, 0, 0);
			
			try {
				icon = a.getDrawable(R.styleable.CustomScaleView_icon_default);
				tintColor = a.getColor(R.styleable.CustomScaleView_icon_fill_color, defaultTintColor);
				elementsCount = a.getInt(R.styleable.CustomScaleView_elements_count, 3);
				tintedElementsCount = a.getInt(R.styleable.CustomScaleView_filled_elements_count, 2);
				iconSideSize = a.getInt(R.styleable.CustomScaleView_icon_side_size, MIN_SIDE);
				internalPadding = a.getInt(R.styleable.CustomScaleView_internal_padding, 0);
			}
			finally {
				a.recycle();
			}
			
			if (icon == null) {
				icon = getContext().getResources().getDrawable(R.drawable.ic_risk_indicator_24dp);
			}
		}
	}
	
	@Override
	protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		int h = icon.getIntrinsicHeight();
		int w = icon.getIntrinsicWidth();
		
		int desiredWidth = Math.max(w, iconSideSize) * elementsCount + (INTERNAL_PADDING * elementsCount-1);
		int desiredHeight = Math.max(h, iconSideSize);
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int width;
		int height;
		
		// EXACTLY - Must be this concrete size, AT_MOST - Can't be bigger than it, UNSPECIFIED - Be whatever you want
		
		if (widthMode == MeasureSpec.EXACTLY) {
			width = widthSize;
		} else if (widthMode == MeasureSpec.AT_MOST) {
			width = Math.min(desiredWidth, widthSize);
		} else {
			width = desiredWidth;
		}
		
		if (heightMode == MeasureSpec.EXACTLY) {
			height = heightSize;
		} else if (heightMode == MeasureSpec.AT_MOST) {
			height = Math.min(desiredHeight, heightSize);
		} else {
			height = desiredHeight;
		}
		
		iconSideSize = desiredHeight;
		
		if (internalPadding != 0) {
			int totalInternalPaddingValue = (elementsCount-1) * internalPadding;
			width = internalPadding > 0 ? width - totalInternalPaddingValue : width + totalInternalPaddingValue;
		}
		
		setMeasuredDimension(width, height);
	}
	
	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		
		int startPoint = 0;
		Drawable currentIcon;
		
		for (int i = 0; i < elementsCount; i++) {
			
			if (i > 0) {
				startPoint = startPoint + iconSideSize;
				startPoint += internalPadding;
			}
			
			currentIcon = allDrawables[i];
			
			if (i < tintedElementsCount) {
				currentIcon = DrawableUtil.getTintedDrawable(getContext(), currentIcon, tintColor);
			} else {
				currentIcon = DrawableUtil.getTintedDrawable(getContext(), currentIcon, defaultColor);
			}
			
			currentIcon.setBounds(startPoint,0, startPoint + iconSideSize, iconSideSize);
			currentIcon.draw(canvas);
		}
	}
	
	public int getMaxElementsCount() {
		return elementsCount;
	}
	
	public void setFilledElementsCount(int value) {
		if (value > elementsCount || value <= 0) {
			return;
		}
		
		tintedElementsCount = value;
		invalidate();
	}
}
