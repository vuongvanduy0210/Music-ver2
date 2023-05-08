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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.vuongvanduy.music.databinding.ActivitySignUpBinding
import com.vuongvanduy.music.dialog.ProgressDialog

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
    }

    private fun initListener() {
        binding.btSignUp.setOnClickListener {
            onClickSignUp()
        }

        binding.layoutSignIn.setOnClickListener {
            onClickSignIn()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun onClickSignUp() {
        hideKeyboard()
        progressDialog = ProgressDialog(this, "Signing up...")

        binding.tvError.apply {
            text = ""
            visibility = View.GONE
        }

        val email = binding.edtEmail.text.trim().toString()
        val name = binding.edtName.text.trim().toString()
        val password = binding.edtPassword.text.trim().toString()
        val confirmPassword = binding.edtConfirmPassword.text.trim().toString()

        if (email.isEmpty() || email.isBlank()) {
            binding.apply {
                tvError.text = "Email can't blank"
                edtEmail.setText("")
                tvError.visibility = View.VISIBLE
            }
            return
        } else if (name.isEmpty() || name.isBlank()) {
            binding.apply {
                tvError.text = "Name can't blank"
                edtName.setText("")
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
        } else if (password != confirmPassword) {
            binding.apply {
                tvError.text = "Those passwords did’t match. Try again."
                edtPassword.setText("")
                edtConfirmPassword.setText("")
                tvError.visibility = View.VISIBLE
            }
            Toast.makeText(this,
                "", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (!signInMethods.isNullOrEmpty()) {
                        // email is exists
                        binding.apply {
                            tvError.text = "Email already exists"
                            edtEmail.setText("")
                            tvError.visibility = View.VISIBLE
                        }
                        return@addOnCompleteListener
                    } else {
                        // available for sign up
                        signUp(email, password, name)
                    }
                }
            }
    }

    @SuppressLint("SetTextI18n")
    private fun signUp(email: String, password: String, name: String) {
        val auth = FirebaseAuth.getInstance()
        progressDialog.show()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    setNameForUser(name)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("SignUpActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            .addOnFailureListener {exception ->
                if (exception is FirebaseAuthInvalidUserException) {
                    binding.tvError.apply {
                        text = "Email already exists"
                        visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun setNameForUser(name: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val nameUpdates = userProfileChangeRequest {
            displayName = name
        }
        user?.updateProfile(nameUpdates)?.addOnCompleteListener {
            progressDialog.dismiss()
            if (it.isSuccessful) {
                val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
        }
    }

    private fun onClickSignIn() {
        val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
        startActivity(intent)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}