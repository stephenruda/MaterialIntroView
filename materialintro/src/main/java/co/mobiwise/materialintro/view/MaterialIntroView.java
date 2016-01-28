package co.mobiwise.materialintro.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import co.mobiwise.materialintro.AnimationFactory;
import co.mobiwise.materialintro.Constants;
import co.mobiwise.materialintro.R;
import co.mobiwise.materialintro.shape.Circle;
import co.mobiwise.materialintro.shape.Focus;
import co.mobiwise.materialintro.shape.FocusGravity;
import co.mobiwise.materialintro.target.Target;
import co.mobiwise.materialintro.target.ViewTarget;

/**
 * Created by mertsimsek on 22/01/16.
 */
public class MaterialIntroView extends RelativeLayout{

    /**
     * Mask color
     */
    private int maskColor;

    /**
     * MaterialIntroView will start
     * showing after delayMillis seconds
     * passed
     */
    private long delayMillis;

    /**
     * We don't draw MaterialIntroView
     * until isReady field set to true
     */
    private boolean isReady;

    /**
     * Show/Dismiss MaterialIntroView
     * with fade in/out animation if
     * this is enabled.
     */
    private boolean isFadeAnimationEnabled;

    /**
     * Animation duration
     */
    private long fadeAnimationDuration;

    /**
     * circleShape focus on target
     * and clear circle to focus
     */
    private Circle circleShape;

    /**
     * Focus Type
     */
    private Focus focusType;

    /**
     * FocusGravity type
     */
    private FocusGravity focusGravity;

    /**
     * Target View
     */
    private Target targetView;

    /**
     * Eraser
     */
    private Paint eraser;

    /**
     * Handler will be used to
     * delay MaterialIntroView
     */
    private Handler handler;

    /**
     * All views will be drawn to
     * this bitmap and canvas then
     * bitmap will be drawn to canvas
     */
    private Bitmap bitmap;
    private Canvas canvas;

    /**
     * Circle padding
     */
    private int padding;

    /**
     * Layout width/height
     */
    private int width;
    private int height;

    /**
     * Dismiss on touch any position
     */
    private boolean dismissOnTouch;

    /**
     * Info dialog view
     */
    private View infoView;

    /**
     * Info Dialog Text
     */
    private TextView textViewInfo;

    /**
     * Info dialog text color
     */
    private int colorTextViewInfo;

    /**
     * Info dialog will be shown
     * If this value true
     */
    private boolean isInfoEnabled;

    /**
     * Dot view will appear center of
     * cleared target area
     */
    private View dotView;

    /**
     * When layout completed, we set this true
     * Otherwise onGlobalLayoutListener stuck on loop.
     */
    private boolean isLayoutCompleted;

    public MaterialIntroView(Context context) {
        super(context);
        init(context);
    }

    public MaterialIntroView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MaterialIntroView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MaterialIntroView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        setWillNotDraw(false);
        setVisibility(INVISIBLE);

        /**
         * set default values
         */
        maskColor = Constants.DEFAULT_MASK_COLOR;
        delayMillis = Constants.DEFAULT_DELAY_MILLIS;
        fadeAnimationDuration = Constants.DEFAULT_FADE_DURATION;
        padding = Constants.DEFAULT_TARGET_PADDING;
        colorTextViewInfo = Constants.DEFAULT_COLOR_TEXTVIEW_INFO;
        focusType = Focus.ALL;
        focusGravity = FocusGravity.CENTER;
        isReady = false;
        isFadeAnimationEnabled = false;
        dismissOnTouch = false;
        isLayoutCompleted = false;
        isInfoEnabled = true;

        /**
         * initialize objects
         */
        handler = new Handler();

        eraser = new Paint();
        eraser.setColor(0xFFFFFFFF);
        eraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraser.setFlags(Paint.ANTI_ALIAS_FLAG);

        View layoutInfo =  LayoutInflater.from(getContext()).inflate(R.layout.material_intro_card, null);

        infoView = layoutInfo.findViewById(R.id.info_layout);
        textViewInfo = (TextView) layoutInfo.findViewById(R.id.textview_info);
        textViewInfo.setTextColor(colorTextViewInfo);

        dotView = LayoutInflater.from(getContext()).inflate(R.layout.dotview, null);
        dotView.measure(MeasureSpec.UNSPECIFIED,MeasureSpec.UNSPECIFIED);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                circleShape.reCalculateAll();
                if(circleShape != null && circleShape.getPoint().y != 0 && !isLayoutCompleted){
                    if(isInfoEnabled)
                        handler.post(() -> setInfoLayout(height, circleShape));
                    handler.post(() -> setDotViewLayout());
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!isReady) return;

        if(bitmap == null || canvas == null){
            if(bitmap != null) bitmap.recycle();

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            this.canvas = new Canvas(bitmap);
        }

        /**
         * Draw mask
         */
        this.canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        this.canvas.drawColor(maskColor);

        /**
         * Clear focus area
         */
        circleShape.draw(this.canvas, eraser, padding);

        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    /**
     * Perform click operation when user
     * touches on target circle.
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float xT = event.getX();
        float yT = event.getY();

        int xV = circleShape.getPoint().x;
        int yV = circleShape.getPoint().y;

        int radius = circleShape.getRadius();

        double dx = Math.pow(xT - xV, 2);
        double dy = Math.pow(yT - yV, 2);

        boolean isTouchOnFocus = (dx + dy) <= Math.pow(radius, 2);

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:

                if(isTouchOnFocus){
                    targetView.getView().setPressed(true);
                    targetView.getView().invalidate();
                }

                return true;
            case MotionEvent.ACTION_UP:

                if(isTouchOnFocus || dismissOnTouch)
                    dismiss();

                if(isTouchOnFocus){
                    targetView.getView().performClick();
                    targetView.getView().setPressed(true);
                    targetView.getView().invalidate();
                    targetView.getView().setPressed(false);
                    targetView.getView().invalidate();
                }

                return true;
            default: break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * Shows material view with fade in
     * animation
     * @param activity
     */
    private void show(Activity activity){

        ((ViewGroup) activity.getWindow().getDecorView()).addView(this);

        setReady(true);

        handler.postDelayed(() -> {
            if(isFadeAnimationEnabled)
                AnimationFactory.animateFadeIn(this, fadeAnimationDuration, () -> setVisibility(VISIBLE));
            else
                setVisibility(VISIBLE);

        },delayMillis);

    }

    /**
     * Dismiss Material Intro View
     */
    private void dismiss(){
        AnimationFactory.animateFadeOut(this, fadeAnimationDuration, () -> setVisibility(INVISIBLE));
    }

    /**
     * locate info card view above/below the
     * circle. If circle's Y coordiante is bigger than
     * Y coordinate of root view, then locate cardview
     * above the circle. Otherwise locate below.
     * @param viewPositionY
     * @param circle
     */
    private void setInfoLayout(int viewPositionY, Circle circle){

        isLayoutCompleted = true;

        if(infoView.getParent() != null)
            ((ViewGroup)infoView.getParent()).removeView(infoView);

        RelativeLayout.LayoutParams infoDialogParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT);

        if(circle.getPoint().y < viewPositionY / 2){
            ((RelativeLayout) infoView).setGravity(Gravity.TOP);
            infoDialogParams.setMargins(0,circle.getPoint().y + circle.getRadius(),0,0);
        }
        else{
            ((RelativeLayout) infoView).setGravity(Gravity.BOTTOM);
            infoDialogParams.setMargins(0,0,0,height - (circle.getPoint().y + circle.getRadius()) + 2 * circle.getRadius());
        }

        infoView.setLayoutParams(infoDialogParams);
        infoView.postInvalidate();

        addView(infoView);

        infoView.setVisibility(VISIBLE);

    }

    private void setDotViewLayout(){

        if(dotView.getParent() != null)
            ((ViewGroup)dotView.getParent()).removeView(dotView);

        RelativeLayout.LayoutParams dotViewLayoutParams = (LayoutParams) generateDefaultLayoutParams();
        dotViewLayoutParams.setMargins(
                circleShape.getPoint().x - (dotView.getMeasuredWidth() / 2),
                circleShape.getPoint().y - (dotView.getMeasuredHeight() / 2),
                0,
                0);
        dotView.setLayoutParams(dotViewLayoutParams);
        dotView.postInvalidate();
        addView(dotView);

        dotView.setVisibility(VISIBLE);
    }

    /**
     *
     *
     * SETTERS
     *
     *
     */

    private void setMaskColor(int maskColor){
        this.maskColor = maskColor;
    }

    private void setDelay(int delayMillis){
        this.delayMillis = delayMillis;
    }

    private void enableFadeAnimation(boolean isFadeAnimationEnabled){
        this.isFadeAnimationEnabled = isFadeAnimationEnabled;
    }

    private void setReady(boolean isReady){
        this.isReady = isReady;
    }

    private void setTarget(Target target){
        targetView = target;
    }

    private void setFocusType(Focus focusType){
        this.focusType = focusType;
    }

    private void setCircle(Circle circleShape){
        this.circleShape = circleShape;
    }

    private void setPadding(int padding){
        this.padding = padding;
    }

    private void setDismissOnTouch(boolean dismissOnTouch){
        this.dismissOnTouch = dismissOnTouch;
    }

    private void setFocusGravity(FocusGravity focusGravity){
        this.focusGravity = focusGravity;
    }

    private void setColorTextViewInfo(int colorTextViewInfo){
        this.colorTextViewInfo = colorTextViewInfo;
        textViewInfo.setTextColor(this.colorTextViewInfo);
    }

    private void setTextViewInfo(String textViewInfo){
        this.textViewInfo.setText(textViewInfo);
    }

    private void setTextViewInfoSize(int textViewInfoSize){
        this.textViewInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, textViewInfoSize);
    }

    private void disableInfoDialog(){
        isInfoEnabled = false;
    }

    /**
     *
     *
     * Builder Class
     *
     *
     */
    public static class Builder{

        private MaterialIntroView materialIntroView;

        private Activity activity;

        private Focus focusType = Focus.MINIMUM;

        public Builder(Activity activity) {
            this.activity = activity;
            materialIntroView = new MaterialIntroView(activity);
        }

        public Builder setMaskColor(int maskColor){
            materialIntroView.setMaskColor(maskColor);
            return this;
        }

        public Builder setDelayMillis(int delayMillis){
            materialIntroView.setDelay(delayMillis);
            return this;
        }

        public Builder enableFadeAnimation(boolean isFadeAnimationEnabled){
            materialIntroView.enableFadeAnimation(isFadeAnimationEnabled);
            return this;
        }

        public Builder setFocusType(Focus focusType){
            materialIntroView.setFocusType(focusType);
            return this;
        }

        public Builder setFocusGravity(FocusGravity focusGravity){
            materialIntroView.setFocusGravity(focusGravity);
            return this;
        }

        public Builder setTarget(View view){
            materialIntroView.setTarget(new ViewTarget(view));
            return this;
        }

        public Builder setTargetPadding(int padding){
            materialIntroView.setPadding(padding);
            return this;
        }

        public Builder setTextColor(int textColor){
            materialIntroView.setColorTextViewInfo(textColor);
            return this;
        }

        public Builder setInfoText(String infoText){
            materialIntroView.setTextViewInfo(infoText);
            return this;
        }

        public Builder setInfoTextSize(int textSize){
            materialIntroView.setTextViewInfoSize(textSize);
            return this;
        }

        public Builder dismissOnTouch(boolean dismissOnTouch){
            materialIntroView.setDismissOnTouch(dismissOnTouch);
            return this;
        }

        public Builder disableInfoDialog(){
            materialIntroView.disableInfoDialog();
            return this;
        }

        public MaterialIntroView build(){
            Circle circle = new Circle(
                    materialIntroView.targetView,
                    materialIntroView.focusType,
                    materialIntroView.focusGravity,
                    materialIntroView.padding);
            materialIntroView.setCircle(circle);
            return materialIntroView;
        }

        public MaterialIntroView show(){
            build().show(activity);
            return materialIntroView;
        }

    }

}