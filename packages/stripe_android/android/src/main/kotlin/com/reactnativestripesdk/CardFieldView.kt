package com.reactnativestripesdk

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.uimanager.events.EventDispatcher
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.stripe.android.databinding.CardInputWidgetBinding
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputListener
import com.stripe.android.view.CardInputWidget
import com.stripe.android.view.CardValidCallback

class CardFieldView(context: ThemedReactContext) : FrameLayout(context) {
  internal var mCardWidget: CardInputWidget = CardInputWidget(context)
  private val cardInputWidgetBinding = CardInputWidgetBinding.bind(mCardWidget)
  val cardDetails: MutableMap<String, Any?> = mutableMapOf("brand" to "", "last4" to "", "expiryMonth" to null, "expiryYear" to null, "postalCode" to "", "validNumber" to "Unknown", "validCVC" to "Unknown", "validExpiryDate" to "Unknown")
  var cardParams: PaymentMethodCreateParams.Card? = null
  var cardAddress: Address? = null
  private var mEventDispatcher: EventDispatcher? = context.getNativeModule(UIManagerModule::class.java)?.eventDispatcher
  private var dangerouslyGetFullCardDetails: Boolean = false
  private var currentFocusedField: String? = null

  init {
    cardInputWidgetBinding.container.isFocusable = true
    cardInputWidgetBinding.container.isFocusableInTouchMode = true
    cardInputWidgetBinding.container.requestFocus()

    addView(mCardWidget)
    setListeners()

    viewTreeObserver.addOnGlobalLayoutListener { requestLayout() }
  }

  fun setAutofocus(value: Boolean) {
    if (value) {
      cardInputWidgetBinding.cardNumberEditText.requestFocus()
      cardInputWidgetBinding.cardNumberEditText.showSoftKeyboard()
    }
  }

  fun requestFocusFromJS() {
    cardInputWidgetBinding.cardNumberEditText.requestFocus()
    cardInputWidgetBinding.cardNumberEditText.showSoftKeyboard()
  }

  fun requestBlurFromJS() {
    cardInputWidgetBinding.cardNumberEditText.hideSoftKeyboard()
    cardInputWidgetBinding.cardNumberEditText.clearFocus()
    cardInputWidgetBinding.container.requestFocus()
  }

  fun requestClearFromJS() {
    cardInputWidgetBinding.cardNumberEditText.setText("")
    cardInputWidgetBinding.cvcEditText.setText("")
    cardInputWidgetBinding.expiryDateEditText.setText("")
    if (mCardWidget.postalCodeEnabled) {
      cardInputWidgetBinding.postalCodeEditText.setText("")
    }
  }

  private fun onChangeFocus() {
    mEventDispatcher?.dispatchEvent(
      CardFocusEvent(id, currentFocusedField))
  }

  fun setCardStyle(value: ReadableMap) {
    val borderWidth = getIntOrNull(value, "borderWidth")
    val backgroundColor = getValOr(value, "backgroundColor", null)
    val borderColor = getValOr(value, "borderColor", null)
    val borderRadius = getIntOrNull(value, "borderRadius") ?: 0
    val textColor = getValOr("value", "textColor", null)
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
  }

  fun setPlaceHolders(value: ReadableMap) {
    val numberPlaceholder = getValOr(value, "number", null)
    val expirationPlaceholder = getValOr(value, "expiration", null)
    val cvcPlaceholder = getValOr(value, "cvc", null)
    val postalCodePlaceholder = getValOr(value, "postalCode", null)

    numberPlaceholder?.let {
      cardInputWidgetBinding.cardNumberEditText.hint = it
    }
    expirationPlaceholder?.let {
      cardInputWidgetBinding.expiryDateEditText.hint = it
    }
    cvcPlaceholder?.let {
      mCardWidget.setCvcLabel(it)
    }
    postalCodePlaceholder?.let {
      cardInputWidgetBinding.postalCodeEditText.hint = it
    }
  }

  fun setDangerouslyGetFullCardDetails(isEnabled: Boolean) {
    dangerouslyGetFullCardDetails = isEnabled
  }

  fun setPostalCodeEnabled(isEnabled: Boolean) {
    mCardWidget.postalCodeEnabled = isEnabled
  }

  fun getValue(): MutableMap<String, Any?> {
    return cardDetails
  }

  private fun onValidCardChange() {
    mCardWidget.paymentMethodCard?.let {
      cardParams = it
      cardAddress = Address.Builder()
        .setPostalCode(cardDetails["postalCode"] as String?)
        .build()
    } ?: run {
      cardParams = null
      cardAddress = null
    }

    mCardWidget.cardParams?.let {
      cardDetails["brand"] = mapCardBrand(it.brand)
      cardDetails["last4"] = it.last4
    } ?: run {
      cardDetails["brand"] = null
      cardDetails["last4"] = null
    }
    sendCardDetailsEvent()
  }

  private fun sendCardDetailsEvent() {
    mEventDispatcher?.dispatchEvent(
      CardChangedEvent(id, cardDetails, mCardWidget.postalCodeEnabled, cardParams != null, dangerouslyGetFullCardDetails))
  }

  private fun setListeners() {
    cardInputWidgetBinding.cardNumberEditText.setOnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.CardNumber.name else null
      onChangeFocus()
    }
    cardInputWidgetBinding.expiryDateEditText.setOnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.ExpiryDate.name else null
      onChangeFocus()
    }
    cardInputWidgetBinding.cvcEditText.setOnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.Cvc.name else null
      onChangeFocus()
    }
    cardInputWidgetBinding.postalCodeEditText.setOnFocusChangeListener { _, hasFocus ->
      currentFocusedField = if (hasFocus) CardInputListener.FocusField.PostalCode.name else null
      onChangeFocus()
    }

    mCardWidget.setCardValidCallback { isValid, invalidFields ->
      cardDetails["validNumber"] = if (invalidFields.contains(CardValidCallback.Fields.Number)) "Invalid" else "Valid"
      cardDetails["validCVC"] = if (invalidFields.contains(CardValidCallback.Fields.Cvc)) "Invalid" else "Valid"
      cardDetails["validExpiryDate"] = if (invalidFields.contains(CardValidCallback.Fields.Expiry)) "Invalid" else "Valid"
      if (isValid) {
        onValidCardChange()
      } else {
        cardParams = null
        cardAddress = null
      }
    }

    mCardWidget.setCardInputListener(object : CardInputListener {
      override fun onCardComplete() {}
      override fun onExpirationComplete() {}
      override fun onCvcComplete() {}
      override fun onPostalCodeComplete() {}
      override fun onFocusChange(focusField: CardInputListener.FocusField) {}
    })

    mCardWidget.setExpiryDateTextWatcher(object : TextWatcher {
      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
      override fun afterTextChanged(p0: Editable?) {}
      override fun onTextChanged(var1: CharSequence?, var2: Int, var3: Int, var4: Int) {
        val splitText = var1.toString().split("/")
        cardDetails["expiryMonth"] = splitText[0].toIntOrNull()

        if (splitText.size == 2) {
          cardDetails["expiryYear"] = var1.toString().split("/")[1].toIntOrNull()
        }

        sendCardDetailsEvent()
      }
    })

    mCardWidget.setPostalCodeTextWatcher(object : TextWatcher {
      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
      override fun afterTextChanged(p0: Editable?) {}
      override fun onTextChanged(var1: CharSequence?, var2: Int, var3: Int, var4: Int) {
        cardDetails["postalCode"] = var1.toString()
        sendCardDetailsEvent()
      }
    })

    mCardWidget.setCardNumberTextWatcher(object : TextWatcher {
      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
      override fun afterTextChanged(p0: Editable?) {}
      override fun onTextChanged(var1: CharSequence?, var2: Int, var3: Int, var4: Int) {
        if (dangerouslyGetFullCardDetails) {
          cardDetails["number"] = var1.toString().replace(" ", "")
        }
        sendCardDetailsEvent()
      }
    })

    mCardWidget.setCvcNumberTextWatcher(object : TextWatcher {
      override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
      override fun afterTextChanged(p0: Editable?) {}
      override fun onTextChanged(var1: CharSequence?, var2: Int, var3: Int, var4: Int) {
        sendCardDetailsEvent()
      }
    })
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
