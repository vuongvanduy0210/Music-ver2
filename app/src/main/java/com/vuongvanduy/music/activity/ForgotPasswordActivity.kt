package com.vuongvanduy.music.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.FirebaseAuth
import com.vuongvanduy.music.databinding.ActivityForgotPasswordBinding
import com.vuongvanduy.music.dialog.ProgressDialog

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)

        initListener()

        setContentView(binding.root)
    }

    private fun initListener() {
        binding.btSend.setOnClickListener {
            onClickSendResetPassword()
        }
        binding.layoutSignIn.setOnClickListener {
            onClickSignIn()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onClickSendResetPassword() {
        hideKeyboard()
        val dialog = ProgressDialog(this, "Loading...")

        val email = binding.edtEmail.text.trim().toString()
        binding.tvNoti.apply {
            text = ""
            visibility = View.GONE
        }
        if (email.isEmpty() || email.isBlank()) {
            binding.apply {
                tvNoti.text = "Email can't blank"
                tvNoti.visibility = View.VISIBLE
            }
            return
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.apply {
                tvNoti.text = "Email is wrong format"
                edtEmail.setText("")
                tvNoti.visibility = View.VISIBLE
            }
            return
        }

        dialog.show()
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                dialog.dismiss()
                if (task.isSuccessful) {
                    binding.tvNoti.apply {
                        text = "Email sent. Check your email to complete reset password."
                        visibility = View.VISIBLE
                    }
                } else {
                    binding.tvNoti.apply {
                        text = "Email sent fail. Please check your email or network connection."
                        visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun onClickSignIn() {
        val intent = Intent(this@ForgotPasswordActivity, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}