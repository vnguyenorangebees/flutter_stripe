package com.reactnativestripesdk

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.FrameLayout
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.uimanager.events.EventDispatcher
import com.flutter.stripe.R
import com.google.android.material.shape.MaterialShapeDrawable
import com.stripe.android.databinding.CardMultilineWidgetBinding
import com.stripe.android.databinding.StripeCardFormViewBinding
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardFormView
import com.stripe.android.view.CardInputListener

class CardFormView(context: ThemedReactContext) : FrameLayout(context) {
  internal var cardForm: CardFormView = CardFormView(context, null, R.style.StripeCardFormView_Borderless)
  private var mEventDispatcher: EventDispatcher? = context.getNativeModule(UIManagerModule::class.java)?.eventDispatcher
  private var dangerouslyGetFullCardDetails: Boolean = false
  private var currentFocusedField: String? = null
  var cardParams: PaymentMethodCreateParams.Card? = null
  var cardAddress: Address? = null
  private val cardFormViewBinding = StripeCardFormViewBinding.bind(cardForm)
  private val multilineWidgetBinding = CardMultilineWidgetBinding.bind(cardFormViewBinding.cardMultilineWidget)

  init {
    cardFormViewBinding.cardMultilineWidgetContainer.isFocusable = true
    cardFormViewBinding.cardMultilineWidgetContainer.isFocusableInTouchMode = true

    addView(cardForm)
    setListeners()

    viewTreeObserver.addOnGlobalLayoutListener { requestLayout() }
  }

  fun setPostalCodeEnabled(value: Boolean) {
    val cardFormView = StripeCardFormViewBinding.bind(cardForm)
    val visibility = if (value) View.VISIBLE else View.GONE

    cardFormView.cardMultilineWidget.postalCodeRequired = false
    cardFormView.postalCodeContainer.visibility = visibility
  }

  // TODO: uncomment when ios-sdk allows for this
  //  fun setPlaceHolders(value: ReadableMap) {
  //    val cardFormView = StripeCardFormViewBinding.bind(cardForm)
  //
  //    val numberPlaceholder = getValOr(value, "number", null)
  //    val expirationPlaceholder = getValOr(value, "expiration", null)
  //    val cvcPlaceholder = getValOr(value, "cvc", null)
  //    val postalCodePlaceholder = getValOr(value, "postalCode", null)
  //
  //    numberPlaceholder?.let {
  ////      multilineWidgetBinding.tlCardNumber.hint = it
  //    }
  //    expirationPlaceholder?.let {
  //      multilineWidgetBinding.tlExpiry.hint = it
  //    }
  //    cvcPlaceholder?.let {
  //      multilineWidgetBinding.tlCvc.hint = it
  //    }
  //    postalCodePlaceholder?.let {
  //      cardFormView.postalCodeContainer.hint = it
  //    }
  //  }

  fun setAutofocus(value: Boolean) {
    if (value) {
      val cardNumberEditText = multilineWidgetBinding.etCardNumber
      cardNumberEditText.requestFocus()
      cardNumberEditText.showSoftKeyboard()
    }
  }

  fun requestFocusFromJS() {
    val cardNumberEditText = multilineWidgetBinding.etCardNumber
    cardNumberEditText.requestFocus()
    cardNumberEditText.showSoftKeyboard()
  }

  fun requestBlurFromJS() {
    val cardNumberEditText = multilineWidgetBinding.etCardNumber
    cardNumberEditText.hideSoftKeyboard()
    cardNumberEditText.clearFocus()
  }

  fun requestClearFromJS() {
    multilineWidgetBinding.etCardNumber.setText("")
    multilineWidgetBinding.etCvc.setText("")
    multilineWidgetBinding.etExpiry.setText("")
    cardFormViewBinding.postalCode.setText("")
  }

  private fun onChangeFocus() {
    mEventDispatcher?.dispatchEvent(
      CardFocusEvent(id, currentFocusedField))
  }

  fun setCardStyle(value: ReadableMap) {
    val binding = StripeCardFormViewBinding.bind(cardForm)
    val backgroundColor = getValOr(value, "backgroundColor", null)
    val borderWidth = getIntOrNull(value, "borderWidth")
    val backgroundColor = getValOr(value, "backgroundColor", null)
    val borderColor = getValOr(value, "borderColor", null)
    val borderRadius = getIntOrNull(value, "borderRadius") ?: 0
    val textColor = getValOr(value, "textColor", null)
    val fontSize = getIntOrNull(value, "fontSize")
    val fontFamily = getValOr(value, "fontFamily")
    val placeholderColor = getValOr(value, "placeholderColor", null)
    val textErrorColor = getValOr(value, "textErrorColor", null)
    val cursorColor = getValOr(value, "cursorColor", null)
    val bindings = setOf(
            cardInputWidgetBinding.cardNumberEditText,
            cardInputWidgetBinding.cvcEditText,
            cardInputWidgetBinding.expiryDateEditText,
            cardInputWidgetBinding.postalCodeEditText)

    textColor?.let {
      for (editTextBinding in bindings) {
        editTextBinding.setTextColor(Color.parseColor(it))
      }
    }
    textErrorColor?.let {
      for (editTextBinding in bindings) {
        editTextBinding.setErrorColor(Color.parseColor(it))
      }
    }
    placeholderColor?.let {
      for (editTextBinding in bindings) {
        editTextBinding.setHintTextColor(Color.parseColor(it))
      }
    }
    fontSize?.let {
      for (editTextBinding in bindings) {
        editTextBinding.textSize = it.toFloat()
      }
    }
    fontFamily?.let {
      for (editTextBinding in bindings) {
        editTextBinding.typeface = Typeface.create(it, Typeface.NORMAL)
      }
    }
    cursorColor?.let {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val color = Color.parseColor(it)
        for (editTextBinding in bindings) {
          editTextBinding.textCursorDrawable?.setTint(color)
          editTextBinding.textSelectHandle?.setTint(color)
          editTextBinding.textSelectHandleLeft?.setTint(color)
          editTextBinding.textSelectHandleRight?.setTint(color)
          editTextBinding.highlightColor = color
        }
      }
    }

    mCardWidget.setPadding(40, 0, 40, 0)
    mCardWidget.background = MaterialShapeDrawable(
            ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, (borderRadius * 2).toFloat())
                    .build()
    ).also { shape ->
      shape.strokeWidth = 0.0f
      shape.strokeColor = ColorStateList.valueOf(Color.parseColor("#000000"))
      shape.fillColor = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
      borderWidth?.let {
        shape.strokeWidth = (it * 2).toFloat()
      }
      borderColor?.let {
        shape.strokeColor = ColorStateList.valueOf(Color.parseColor(it))
      }
      backgroundColor?.let {
        shape.fillColor = ColorStateList.valueOf(Color.parseColor(it))
      }
    }

    cardFormViewBinding.cardMultilineWidgetContainer.background = MaterialShapeDrawable(
            ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, PixelUtil.toPixelFromDIP(borderRadius.toDouble()))
                    .build()
    ).also { shape ->
      shape.strokeWidth = 0.0f
      shape.strokeColor = ColorStateList.valueOf(Color.parseColor("#000000"))
      shape.fillColor = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
      borderWidth?.let {
        shape.strokeWidth = PixelUtil.toPixelFromDIP(it.toDouble())
      }
      borderColor?.let {
        shape.strokeColor = ColorStateList.valueOf(Color.parseColor(it))
      }
      backgroundColor?.let {
        shape.fillColor = ColorStateList.valueOf(Color.parseColor(it))
      }
    }
  }

  fun setDangerouslyGetFullCardDetails(isEnabled: Boolean) {
    dangerouslyGetFullCardDetails = isEnabled
  }

  private fun setListeners() {
    cardForm.setCardValidCallback { isValid, _ ->
      if (isValid) {
        cardForm.cardParams?.let {
          val cardParamsMap = it.toParamMap()["card"] as HashMap<*, *>
          val cardDetails: MutableMap<String, Any> = mutableMapOf(
            "expiryMonth" to cardParamsMap["exp_month"] as Int,
            "expiryYear" to cardParamsMap["exp_year"] as Int,
            "last4" to it.last4,
            "brand" to mapCardBrand(it.brand),
            "postalCode" to (it.address?.postalCode ?: ""),
            "country" to (it.address?.country ?: "")
          )

          if (dangerouslyGetFullCardDetails) {
            cardDetails["number"] = cardParamsMap["number"] as String
          }

          mEventDispatcher?.dispatchEvent(
            CardFormCompleteEvent(id, cardDetails, isValid, dangerouslyGetFullCardDetails))

          cardAddress = Address.Builder()
            .setPostalCode(it.address?.postalCode)
            .setCountry(it.address?.country)
            .build()

          val binding = StripeCardFormViewBinding.bind(cardForm)
          binding.cardMultilineWidget.paymentMethodCard?.let { params -> cardParams = params }
        }
      } else {
        cardParams = null
        cardAddress = null
        mEventDispatcher?.dispatchEvent(
          CardFormCompleteEvent(id, null, isValid, dangerouslyGetFullCardDetails))
      }
    }

    val cardNumberEditText = multilineWidgetBinding.etCardNumber
    val cvcEditText = multilineWidgetBinding.etCvc
    val expiryEditText = multilineWidgetBinding.etExpiry
    val postalCodeEditText = cardFormViewBinding.postalCode

    cardNumberEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.CardNumber.toString() else  null
      onChangeFocus()
    }
    cvcEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.Cvc.toString() else  null
      onChangeFocus()
    }
    expiryEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.ExpiryDate.toString() else  null
      onChangeFocus()
    }
    postalCodeEditText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.PostalCode.toString() else  null
      onChangeFocus()
    }
  }

  override fun requestLayout() {
    super.requestLayout()
    post(mLayoutRunnable)
  }

  private val mLayoutRunnable = Runnable {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    layout(left, top, right, bottom)
  }
}
