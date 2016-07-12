package com.kongdy.hasee.noendlessruler.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * 尺子,无限瀑布流
 * @author kongdy
 * @date 2016-07-12 20:13
 * @TIME 20:13
 **/

public class NoEndlessRuler extends RecyclerView {

    public NoEndlessRuler(Context context) {
        super(context);
        init();
    }

    public NoEndlessRuler(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NoEndlessRuler(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * 初始化
     */
    private void init() {

    }


    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
    }


}
