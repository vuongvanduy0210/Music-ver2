package com.vuongvanduy.music.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.vuongvanduy.music.databinding.FragmentChangePasswordBinding
import com.vuongvanduy.music.dialog.ProgressDialog

class ChangePasswordFragment : Fragment() {

    private lateinit var binding: FragmentChangePasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangePasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        binding.btChangePassword.setOnClickListener {
            onClickChangePassword()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onClickChangePassword() {
        hideKeyboard()
        val dialog = ProgressDialog(requireActivity(), "Loading...")

        binding.apply {
            tvError.text = ""
            tvError.visibility = View.GONE
            tvNoti.visibility = View.GONE
        }

        val oldPass = binding.edtOldPassword.text.trim().toString()
        val newPass = binding.edtNewPassword.text.trim().toString()
        val confirmPass = binding.edtConfirmPassword.text.trim().toString()

        if (oldPass.isEmpty() || oldPass.isBlank()) {
            binding.apply {
                tvError.text = "Password can't blank"
                edtNewPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (newPass.isEmpty() || newPass.isBlank()) {
            binding.apply {
                tvError.text = "New password can't blank"
                edtNewPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (confirmPass.isEmpty() || confirmPass.isBlank()) {
            binding.apply {
                tvError.text = "Confirm password can't blank"
                edtConfirmPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (newPass.length < 6) {
            binding.apply {
                tvError.text = "Password must contain at least 6 characters"
                edtNewPassword.setText("")
                edtConfirmPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (newPass != confirmPass) {
            binding.apply {
                tvError.text = "Those passwords did’t match. Try again."
                edtNewPassword.setText("")
                edtConfirmPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(user?.email!!, oldPass)
        dialog.show()
        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                dialog.dismiss()
                if (authTask.isSuccessful) {
                    dialog.show()
                    user.updatePassword(newPass)
                        .addOnCompleteListener { task ->
                            dialog.dismiss()
                            if (task.isSuccessful) {
                                binding.apply {
                                    edtOldPassword.setText("")
                                    edtNewPassword.setText("")
                                    edtConfirmPassword.setText("")
                                }
                                binding.tvNoti.text = "Change password success."
                                binding.tvNoti.visibility = View.VISIBLE
                            }
                        }
                } else {
                    // If authentication fails, show an error message
                    binding.apply {
                        tvError.text = "Password is incorrect."
                        tvError.visibility = View.VISIBLE
                        edtOldPassword.setText("")
                    }
                }
            }
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}