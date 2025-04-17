package id.vern.wincross.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import id.vern.wincross.databinding.DialogBlurBinding

class BlurDialogFragment(
    private val title: String,
    private val message: String,
    private val positiveAction: () -> Unit
) : DialogFragment() {

  private lateinit var binding: DialogBlurBinding

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = DialogBlurBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    dialog?.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

    binding.dialogTitle.text = title
    binding.dialogMessage.text = message

    binding.btnYes.setOnClickListener {
      positiveAction()
      dismiss()
    }

    binding.btnCancel.setOnClickListener { dismiss() }
  }
}
