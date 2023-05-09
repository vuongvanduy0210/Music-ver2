package com.vuongvanduy.music.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.vuongvanduy.music.databinding.ActivitySignInBinding
import com.vuongvanduy.music.dialog.ProgressDialog

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
    }

    private fun initListener() {
        binding.btSignIn.setOnClickListener {
            onClickSignIn()
        }

        binding.layoutSignUp.setOnClickListener {
            onClickSignUp()
        }

        binding.btGuest.setOnClickListener {
            onClickBtGuest()
        }

        binding.layoutForgotPassword.setOnClickListener {
            onClickForgotPassword()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onClickSignIn() {
        hideKeyboard()
        val progressDialog = ProgressDialog(this, "Signing in...")

        binding.tvError.apply {
            text = ""
            visibility = View.GONE
        }

        val auth = FirebaseAuth.getInstance()

        val email = binding.edtEmail.text.trim().toString()
        val password = binding.edtPassword.text.trim().toString()

        if (email.isEmpty() || email.isBlank()) {
            binding.apply {
                tvError.text = "Email can't blank"
                edtEmail.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (password.isEmpty() || password.isBlank()) {
            binding.apply {
                tvError.text = "Password can't blank"
                edtPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.apply {
                tvError.text = "Email is wrong format"
                edtEmail.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (password.length < 6) {
            binding.apply {
                tvError.text = "Password must contain at least 6 characters"
                edtPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        }

        progressDialog.show()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("LoginActivity", "signInWithEmail:success")
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                }
            }
            .addOnFailureListener { exception ->
                if (exception is FirebaseAuthInvalidUserException) {
                    binding.tvError.apply {
                        text = "Email does not exist"
                        visibility = View.VISIBLE
                    }
                } else if (exception is FirebaseAuthInvalidCredentialsException) {
                    binding.tvError.apply {
                        text = "Password is incorrect"
                        visibility = View.VISIBLE
                    }
                    binding.edtPassword.setText("")
                }
            }
    }

    private fun onClickSignUp() {
        val intent = Intent(this@SignInActivity, SignUpActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun onClickBtGuest() {
        val intent = Intent(this@SignInActivity, MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun onClickForgotPassword() {
        val intent = Intent(this@SignInActivity, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }



    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}