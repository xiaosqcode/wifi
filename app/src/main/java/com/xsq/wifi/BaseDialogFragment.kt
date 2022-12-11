package com.xsq.wifi

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDialogFragment

/**
 * 基础对话框
 *
 * Created by Xiaoshiquan on 2021/4/9.
 */
open class BaseDialogFragment : AppCompatDialogFragment() {

    private var _dialog: Dialog? = null
    private var _cancelable: Boolean = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = _cancelable
        return _dialog ?: super.onCreateDialog(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 对话框构建器
     *
     * @param context 上下文，需要传入 Activity 对象
     * @param themeResId
     */
    class Builder(context: Context, themeResId: Int) {

        private val dialogBuilder = AlertDialog.Builder(context, themeResId)
        private var cancelable = true

        /**
         * 获取对话框构建对象
         */
        fun builder(): AlertDialog.Builder {
            return dialogBuilder
        }

        /**
         * 设置标题
         *
         * @param title 标题文字
         * @return 构建器对象
         */
        fun setTitle(title: String): Builder {
            dialogBuilder.setTitle(title)
            return this
        }

        /**
         * 设置标题
         *
         * @param titleResId 标题文字资源编号
         * @return 构建器对象
         */
        fun setTitle(titleResId: Int): Builder {
            dialogBuilder.setTitle(titleResId)
            return this
        }

        /**
         * 设置信息
         *
         * @param message 信息文字
         * @return 构建器对象
         */
        fun setMessage(message: String): Builder {
            dialogBuilder.setMessage(message)
            return this
        }

        /**
         * 设置信息
         *
         * @param messageResId 信息文字资源编号
         * @return 构建器对象
         */
        fun setMessage(messageResId: Int): Builder {
            dialogBuilder.setMessage(messageResId)
            return this
        }

        /**
         * 设置是否可以点击对话框外的区域关闭对话框
         *
         * @param cancelable 是否允许点击外部区域关闭
         * @return 构建器对象
         */
        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        /**
         * 设置上一步按钮
         *
         * @param text 按钮文字
         * @param listener 点击事件监听器
         * @return 构建器对象
         */
        fun setNegativeButton(text: String, listener: DialogInterface.OnClickListener): Builder {
            dialogBuilder.setNegativeButton(text, listener)
            return this
        }

        /**
         * 设置上一步按钮
         *
         * @param textResId 按钮文字资源编号
         * @param listener 点击事件监听器
         * @return 构建器对象
         */
        fun setNegativeButton(textResId: Int, listener: DialogInterface.OnClickListener): Builder {
            dialogBuilder.setNegativeButton(textResId, listener)
            return this
        }

        /**
         * 设置中间选择按钮
         *
         * @param text 按钮文字
         * @param listener 点击事件监听器
         * @return 构建器对象
         */
        fun setNeutralButton(text: String, listener: DialogInterface.OnClickListener): Builder {
            dialogBuilder.setNeutralButton(text, listener)
            return this
        }

        /**
         * 设置中间选择按钮
         *
         * @param textResId 按钮文字资源编号
         * @param listener 点击事件监听器
         * @return 构建器对象
         */
        fun setNeutralButton(textResId: Int, listener: DialogInterface.OnClickListener): Builder {
            dialogBuilder.setNeutralButton(textResId, listener)
            return this
        }

        /**
         * 设置下一步按钮
         *
         * @param text 按钮文字
         * @param listener 点击事件监听器
         * @return 构建器对象
         */
        fun setPositiveButton(text: String, listener: DialogInterface.OnClickListener): Builder {
            dialogBuilder.setPositiveButton(text, listener)
            return this
        }

        /**
         * 设置下一步按钮
         *
         * @param textResId 按钮文字资源编号
         * @param listener 点击事件监听器
         * @return 构建器对象
         */
        fun setPositiveButton(textResId: Int, listener: DialogInterface.OnClickListener): Builder {
            dialogBuilder.setPositiveButton(textResId, listener)
            return this
        }

        /**
         * 设置一个列表，并设置选中的项
         *
         * @param array 可供选择的数组
         * @param checkedItem 当前选中的元素下标，未选中任一项则传入 -1
         * @param listener item点击事件，通过 [DialogInterface.OnClickListener.onClick] 中的 which 参数传递被点击的元素下标
         */
        fun setSingleChoiceItems(array: Array<String>, checkedItem: Int, listener: DialogInterface.OnClickListener): Builder {
            dialogBuilder.setSingleChoiceItems(array, checkedItem, listener)
            return this
        }

        /**
         * 设置一个列表，并设置选中的项
         *
         * @param array 可供选择的数组
         * @param checkedItems 数组中的元素的选择状态，长度需要与 [array] 长度对应
         * @param listener item选择事件，通过 [DialogInterface.OnMultiChoiceClickListener.onClick] 中的 which 参数传递被点击的元素下标，isChecked 参数表示选中状态
         */
        fun setMultiChoiceItems(array: Array<String>, checkedItems: BooleanArray, listener: DialogInterface.OnMultiChoiceClickListener): Builder {
            dialogBuilder.setMultiChoiceItems(array, checkedItems, listener)
            return this
        }

        /**
         * 设置自定义视图
         *
         * @param view 自定义视图对象
         * @return 构建器对象
         */
        fun setView(view: View): Builder {
            dialogBuilder.setView(view)
            return this
        }

        /**
         * 设置自定义视图
         *
         * @param viewResId 自定义视图资源编号
         * @return 构建器对象
         */
        fun setView(viewResId: Int): Builder {
            dialogBuilder.setView(viewResId)
            return this
        }

        /**
         * 创建
         *
         * @return 对话框Fragment对象
         */
        fun create(): BaseDialogFragment {
            val dialogFragment = BaseDialogFragment()
            dialogFragment._dialog = dialogBuilder.create()
            dialogFragment._cancelable = cancelable
            return dialogFragment
        }

    }

}