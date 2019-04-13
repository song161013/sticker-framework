package com.whensunset.test;

import android.app.Service;
import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.whensunset.sticker.DecorationElement;
import com.whensunset.sticker.DecorationElementContainerView;

/**
 * Created by whensunset on 2019/4/14.
 * 仿写抖音的 ECV
 */

public class MyTikTokElementContainerView extends DecorationElementContainerView {
  private static final String TAG = "heshixi:MTTECV";
  private static final float IS_CHECK_X_RULE_THRESHOLD = 8; // 在 x 轴移动元素 deltaX 低于阈值的时候进行移动规则监测
  private static final float IS_CHECK_Y_RULE_THRESHOLD = 8; // 在 y 轴移动元素 deltaY 低于阈值的时候进行移动规则监测
  private static final float CHECK_X_IS_IN_RULE_THRESHOLD = 2; // 检测元素的 x 是否在规则中的阈值
  private static final float CHECK_Y_IS_IN_RULE_THRESHOLD = 2; // 检测元素的 y 是否在规则中的阈值
  private static final float X_RULE_TOTAL_ABSORPTION_MAX = 60; // 某次 x 轴移动规则累计吸收的 x 轴移动距离的最大值
  private static final float Y_RULE_TOTAL_ABSORPTION_MAX = 60; // 某次 y 轴移动规则累计吸收的 y 轴移动距离的最大值
  private static final long VIBRATOR_DURATION = 10; // 震动的时长
  private static final float NOT_IN_RULE = -1; // 当前元素不处于任何规则中
  private static final float[] X_RULES = new float[]{0.05f, 0.5f, 0.95f}; // x 轴上的规则监测点，单位为 view 的百分比
  private static final float[] Y_RULES = new float[]{0.10f, 0.5f, 0.90f}; // y 轴上的规则监测点，单位为 view 的百分比
  
  private RuleLineView mRuleLineView;
  RuleLineView.RuleLine[] mRuleLines = new RuleLineView.RuleLine[2];
  private float mXRuleTotalAbsorption = 0; // 某次 x 轴移动规则累计吸收的 x 轴移动距离
  private float mYRuleTotalAbsorption = 0; // 某次 y 轴移动规则累计吸收的 y 轴移动距离
  private Vibrator mVibrator;
  
  public MyTikTokElementContainerView(Context context) {
    super(context);
  }
  
  public MyTikTokElementContainerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  public MyTikTokElementContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }
  
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public MyTikTokElementContainerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }
  
  @Override
  protected void viewLayoutComplete() {
    super.viewLayoutComplete();
    mRuleLineView = new RuleLineView(getContext());
    LayoutParams layoutParams = new LayoutParams(getWidth(), getHeight(), 0, 0);
    mRuleLineView.setLayoutParams(layoutParams);
    mRuleLineView.setRuleLines(mRuleLines);
    addView(mRuleLineView);
    mRuleLineView.setVisibility(GONE);
    mVibrator = (Vibrator)getContext().getSystemService(Service.VIBRATOR_SERVICE);
  }
  
  @Override
  protected boolean scrollSelectTapOtherAction(@NonNull MotionEvent event, float[] distance) {
    if (mSelectedElement == null) {
      Log.w(TAG, "scrollSelectTapOtherAction mSelectedElement is null");
      return super.scrollSelectTapOtherAction(event, distance);
    }
    
    boolean xCanCheckRule = (Math.abs(distance[0]) <= IS_CHECK_X_RULE_THRESHOLD);
    boolean yCanCheckRule = (Math.abs(distance[1]) <= IS_CHECK_Y_RULE_THRESHOLD);
    boolean xInRule = false;
    boolean yInRule = false;
    
    float xRulePercent = 0;
    if (xCanCheckRule) {
      xRulePercent = checkElementInXRule();
      xInRule = (xRulePercent != NOT_IN_RULE);
      if (xInRule) {
        RuleLineView.RuleLine xRuleLine = new RuleLineView.RuleLine();
        xRuleLine.mStartPoint = new PointF(xRulePercent * getWidth(), 0);
        xRuleLine.mEndPoint = new PointF(xRulePercent * getWidth(), getHeight());
        mRuleLines[0] = xRuleLine;
  
        if (mXRuleTotalAbsorption == 0) {
          mVibrator.vibrate(VIBRATOR_DURATION);
          Log.d(TAG, "scrollSelectTapOtherAction x vibrate");
        }
        mXRuleTotalAbsorption += distance[0];
        if (Math.abs(mXRuleTotalAbsorption) >= X_RULE_TOTAL_ABSORPTION_MAX) {
          Log.d(TAG, "scrollSelectTapOtherAction clear mXRuleTotalAbsorption:" + mXRuleTotalAbsorption);
          mXRuleTotalAbsorption = 0;
          distance[0] += (distance[0] < 0 ? -2 * CHECK_X_IS_IN_RULE_THRESHOLD : 2 * CHECK_X_IS_IN_RULE_THRESHOLD);
        } else {
          distance[0] = 0;
          Log.d(TAG, "scrollSelectTapOtherAction add mXRuleTotalAbsorption |||||||||| mXRuleTotalAbsorption:" + mXRuleTotalAbsorption);
        }
      } else {
        mRuleLines[0] = null;
      }
    } else {
      mRuleLines[0] = null;
      mXRuleTotalAbsorption = 0;
    }
    
    float yRulePercent = 0;
    if (yCanCheckRule) {
      yRulePercent = checkElementInYRule();
      yInRule = (yRulePercent != NOT_IN_RULE);
      if (yInRule) {
        RuleLineView.RuleLine yRuleLine = new RuleLineView.RuleLine();
        yRuleLine.mStartPoint = new PointF(0, yRulePercent * getHeight());
        yRuleLine.mEndPoint = new PointF(getWidth(), yRulePercent * getHeight());
        mRuleLines[1] = yRuleLine;
    
        if (mYRuleTotalAbsorption == 0){
          mVibrator.vibrate(VIBRATOR_DURATION);
          Log.d(TAG, "scrollSelectTapOtherAction y vibrate");
        }
        mYRuleTotalAbsorption += distance[1];
        if (Math.abs(mYRuleTotalAbsorption) >= Y_RULE_TOTAL_ABSORPTION_MAX) {
          Log.d(TAG, "scrollSelectTapOtherAction clear mYRuleTotalAbsorption:" + mYRuleTotalAbsorption);
          mYRuleTotalAbsorption = 0;
          distance[1] += (distance[1] < 0 ? -2 * CHECK_Y_IS_IN_RULE_THRESHOLD : 2 * CHECK_Y_IS_IN_RULE_THRESHOLD);
        } else {
          distance[1] = 0;
          Log.d(TAG, "scrollSelectTapOtherAction add mYRuleTotalAbsorption |||||||||| mYRuleTotalAbsorption:" + mYRuleTotalAbsorption);
        }
      } else {
        mRuleLines[1] = null;
      }
    } else {
      mRuleLines[1] = null;
      mYRuleTotalAbsorption = 0;
    }
    
    if ((xCanCheckRule && xInRule) || (yCanCheckRule && yInRule)) {
      mRuleLineView.setVisibility(VISIBLE);
      mRuleLineView.invalidate();
    } else {
      mRuleLineView.setVisibility(GONE);
    }
    return super.scrollSelectTapOtherAction(event, distance);
  }
  
  @Override
  protected boolean upSelectTapOtherAction(@NonNull MotionEvent event) {
    mRuleLineView.setVisibility(GONE);
    return super.upSelectTapOtherAction(event);
  }
  
  /**
   * 检查当前元素是否处于哪条规则中view 中心的时候可以进行提醒
   *
   * @return 返回当前元素处于哪条规则中
   */
  private float checkElementInXRule() {
    if (mSelectedElement == null) {
      Log.w(TAG, "checkElementInXRule mSelectedElement is null");
      return NOT_IN_RULE;
    }
    
    DecorationElement decorationElement = (DecorationElement) mSelectedElement;
    if (!mSelectedElement.isSingerFingerMove()) {
      return NOT_IN_RULE;
    }
    float elementCenterX = decorationElement.getContentRect().centerX();
    float viewCenterX = getWidth() * X_RULES[1];
    if (Math.abs(viewCenterX - elementCenterX) < CHECK_X_IS_IN_RULE_THRESHOLD) {
      return X_RULES[1];
    }
    
    float elementLeft = decorationElement.getContentRect().left;
    float viewLeft = getWidth() * X_RULES[0];
    if (Math.abs(viewLeft - elementLeft) < CHECK_X_IS_IN_RULE_THRESHOLD) {
      return X_RULES[0];
    }
    
    float elementRight = decorationElement.getContentRect().right;
    float viewRight = getWidth() * X_RULES[2];
    if (Math.abs(viewRight - elementRight) < CHECK_X_IS_IN_RULE_THRESHOLD) {
      return X_RULES[2];
    }
    
    return NOT_IN_RULE;
  }
  
  /**
   * 同 checkElementInXRule
   */
  private float checkElementInYRule() {
    if (mSelectedElement == null) {
      Log.w(TAG, "checkElementInYRule mSelectedElement is null");
      return NOT_IN_RULE;
    }
    
    DecorationElement decorationElement = (DecorationElement) mSelectedElement;
    if (!mSelectedElement.isSingerFingerMove()) {
      return NOT_IN_RULE;
    }
    float elementCenterY = decorationElement.getContentRect().centerY();
    float viewCenterY = getHeight() * Y_RULES[1];
    if (Math.abs(viewCenterY - elementCenterY) < CHECK_Y_IS_IN_RULE_THRESHOLD) {
      return Y_RULES[1];
    }
    
    float elementTop = decorationElement.getContentRect().top;
    float viewTop = getHeight() * Y_RULES[0];
    if (Math.abs(viewTop - elementTop) < CHECK_Y_IS_IN_RULE_THRESHOLD) {
      return Y_RULES[0];
    }
    
    float elementBottom = decorationElement.getContentRect().bottom;
    float viewBottom = getHeight() * Y_RULES[2];
    if (Math.abs(viewBottom - elementBottom) < CHECK_Y_IS_IN_RULE_THRESHOLD) {
      return Y_RULES[2];
    }
    
    return NOT_IN_RULE;
  }
  
  public interface MyTikTokElementActionListener extends DecorationElementActionListener {
  }
  
  public class DefaultMyTikTokElementActionListener extends DefaultDecorationElementActionListener implements MyTikTokElementActionListener {
  }
  
}