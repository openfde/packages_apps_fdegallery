package com.fde.gallery3d.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.fde.gallery3d.R

/**
 * 左侧导航栏中的「智能图册」可展开二级菜单。
 * 点击父级标题时以抽屉动画方式展开 / 收起子菜单（人像 / 风景 / 美食），
 * 选中子项时高亮对应条目。
 */
class SmartAlbumMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var submenu: LinearLayout
    private lateinit var arrow: ImageView

    private var expanded = false
    private var animator: ValueAnimator? = null

    /** 点击子菜单条目时回调，参数为目标导航目的地 id（R.id.nav_category_*）。 */
    var onCategorySelected: ((Int) -> Unit)? = null

    /** 子菜单条目（layout id 与对应导航目的地 id）。 */
    private val categoryItems = listOf(
        R.id.smart_album_item_portrait to R.id.nav_category_portrait,
        R.id.smart_album_item_landscape to R.id.nav_category_landscape,
        R.id.smart_album_item_food to R.id.nav_category_food
    )

    init {
        orientation = VERTICAL
        View.inflate(context, R.layout.view_smart_album_menu, this)

        val header = findViewById<View>(R.id.smart_album_header)
        header.setOnClickListener { toggle() }
        arrow = findViewById(R.id.smart_album_arrow)
        submenu = findViewById(R.id.smart_album_submenu)

        categoryItems.forEach { (rowId, destId) ->
            findViewById<View>(rowId).setOnClickListener {
                onCategorySelected?.invoke(destId)
            }
        }
    }

    /** 根据当前导航目的地刷新选中态，若进入子分类则自动展开。 */
    fun setChecked(destinationId: Int) {
        val isCategory = categoryItems.any { it.second == destinationId }
        if (isCategory && !expanded) {
            expand()
        }
        categoryItems.forEach { (rowId, destId) ->
            findViewById<View>(rowId).isSelected = destId == destinationId
        }
    }

    private fun toggle() {
        if (animator?.isRunning == true) {
            return
        }
        if (expanded) collapse() else expand()
    }

    private fun expand() {
        expanded = true
        submenu.visibility = View.VISIBLE
        val params = submenu.layoutParams
        val target = measureTargetHeight()
        animator = ValueAnimator.ofInt(0, target).apply {
            duration = EXPAND_DURATION
            addUpdateListener {
                params.height = it.animatedValue as Int
                submenu.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (expanded) {
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        submenu.layoutParams = params
                    }
                    animator = null
                }
            })
            start()
        }
        animateArrow(0f, 180f)
    }

    private fun collapse() {
        expanded = false
        val params = submenu.layoutParams
        val start = params.height.takeIf { it > 0 } ?: submenu.measuredHeight
        animator = ValueAnimator.ofInt(start, 0).apply {
            duration = COLLAPSE_DURATION
            addUpdateListener {
                params.height = it.animatedValue as Int
                submenu.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    params.height = 0
                    submenu.layoutParams = params
                    submenu.visibility = View.GONE
                    animator = null
                }
            })
            start()
        }
        animateArrow(180f, 0f)
    }

    private fun measureTargetHeight(): Int {
        val parent = parent as? View
        val widthSpec = MeasureSpec.makeMeasureSpec(
            parent?.width ?: width,
            MeasureSpec.AT_MOST
        )
        submenu.measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        return submenu.measuredHeight
    }

    private fun animateArrow(fromDegrees: Float, toDegrees: Float) {
        ObjectAnimator.ofFloat(arrow, View.ROTATION, fromDegrees, toDegrees).apply {
            duration = 220
            start()
        }
    }

    companion object {
        private const val EXPAND_DURATION = 260L
        private const val COLLAPSE_DURATION = 200L
    }
}
