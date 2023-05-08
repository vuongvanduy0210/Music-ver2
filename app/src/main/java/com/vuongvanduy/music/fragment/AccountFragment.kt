package com.vuongvanduy.music.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.vuongvanduy.music.R
import com.vuongvanduy.music.activity.SignInActivity
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.activity.SignUpActivity
import com.vuongvanduy.music.databinding.FragmentAccountBinding
import com.vuongvanduy.music.dialog.ProgressDialog
import com.vuongvanduy.music.util.FRAGMENT_CHANGE_PASSWORD
import com.vuongvanduy.music.viewmodel.AccountViewModel


class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding

    private lateinit var viewModel: AccountViewModel

    private lateinit var activity: MainActivity

    private var uriImage: Uri? = null

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                Log.e("FRAGMENT_NAME", "Permission denied")
            }
        }

    private val activityResultGetImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // xử lý ảnh được chọn ở đây
                val intent = result.data ?: return@registerForActivityResult
                val uri = intent.data
                if (uri != null) {
                    uriImage = uri
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(activity)[AccountViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDataViewModel()

        initUI()

        initListener()
    }

    private fun setDataViewModel() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            viewModel.setUser(user)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            showViewSignOut(user)
        } else {
            showViewSignIn()

        }
        viewModel.getUser().observe(activity) {
            if (it != null) {
                showViewSignOut(it)
            } else {
                showViewSignIn()
            }
        }
    }

    private fun initListener() {
        binding.btSignIn.setOnClickListener {
            val intent = Intent(activity, SignInActivity::class.java)
            startActivity(intent)
        }
        binding.btSignUp.setOnClickListener {
            val intent = Intent(activity, SignUpActivity::class.java)
            startActivity(intent)
        }
        binding.btSignOut.setOnClickListener {
            AlertDialog.Builder(activity)
                .setTitle("Sign Out")
                .setMessage("Are you sure want to sign out?")
                .setPositiveButton("Yes") { _, _ ->
                    Firebase.auth.signOut()
                    val intent = Intent(activity, SignInActivity::class.java)
                    startActivity(intent)
                    activity.finishAffinity()
                    viewModel.setUser(null)
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }
        binding.imgUser.setOnClickListener {
            requestPermissionReadStorage()
        }
        binding.btUpdateProfile.setOnClickListener {
            onClickUpdateProfile()
        }
        binding.btChangePassword.setOnClickListener {
            onClickChangePassword()
        }
    }

    private fun requestPermissionReadStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            openGallery()
            return
        }
        if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            activityResultLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        activityResultGetImage
            .launch(Intent.createChooser(intent, "Select picture"))
    }

    private fun onClickUpdateProfile() {
        val dialog = ProgressDialog(activity, "Updating...")
        hideKeyboard()
        val name = binding.edtName.text.trim().toString()
        if (name.isEmpty() || name.isBlank()) {
            return
        }
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val profileUpdates = userProfileChangeRequest {
            displayName = name
            if (uriImage != null) {
                photoUri = uriImage
            }
        }
        dialog.show()
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                dialog.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(activity,
                        "Update profile success.",
                        Toast.LENGTH_SHORT).show()
                    setDataViewModel()
                    activity.viewModel.setUser(FirebaseAuth.getInstance().currentUser)
                }
            }
        uriImage = null
    }

    private fun onClickChangePassword() {
        replaceFragment()
    }

    @SuppressLint("CommitTransaction")
    private fun replaceFragment() {
        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, ChangePasswordFragment()).
        addToBackStack(null).commit()
        activity.currentFragment = FRAGMENT_CHANGE_PASSWORD
    }

    @SuppressLint("SetTextI18n")
    private fun showViewSignIn() {
        binding.apply {
            layoutButtonSignIn.visibility = View.VISIBLE
            layoutButtonSignOut.visibility = View.GONE
            edtName.isEnabled = false
            imgUser.isEnabled = false

            binding.apply {
                Glide.with(activity).load(R.drawable.img_avatar_error).into(imgUser)
                edtName.setText("Guest")
                tvEmail.text = "Someone@gmail.com"
            }
        }
    }

    private fun showViewSignOut(user: FirebaseUser) {
        binding.apply {
            layoutButtonSignIn.visibility = View.GONE
            layoutButtonSignOut.visibility = View.VISIBLE
            edtName.isEnabled = true
            imgUser.isEnabled = true

            Glide.with(activity).load(user.photoUrl).error(R.drawable.img_avatar_error).into(imgUser)
            edtName.setText(user.displayName)
            tvEmail.text = user.email
        }
    }

    override fun onPause() {
        super.onPause()
        Log.e("AccountFragment", "onPause")
    }

    override fun onResume() {
        super.onResume()
        setDataViewModel()
        uriImage?.let { setImageViewForUser(it) }
    }

    private fun setImageViewForUser(uri: Uri) {
        Log.e("AccountFragment", uri.toString())
        Glide.with(activity).load(uri).into(binding.imgUser)
    }

    private fun hideKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view: View = binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}