package com.vuongvanduy.music.activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
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
        binding.layoutSignUp.setOnClickListener {
            val intent = Intent(this@ForgotPasswordActivity, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onClickSendResetPassword() {
        val email = binding.edtEmail.text.trim().toString()
        binding.tvNoti.text = ""
        binding.tvNoti.visibility = View.GONE
        val dialog = ProgressDialog(this, "Loading...")

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
        } else {
            dialog.show()
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    dialog.dismiss()
                    if (task.isSuccessful) {
                        binding.tvNoti.text = "Email sent. Check your email to complete reset password."
                        binding.tvNoti.visibility = View.VISIBLE
                    } else {
                        binding.tvNoti.text = "Email sent fail. Please check your email or network connection."
                        binding.tvNoti.visibility = View.VISIBLE
                    }
                }
        }
    }
}