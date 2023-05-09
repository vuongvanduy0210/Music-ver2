package com.vuongvanduy.music.fragment

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.vuongvanduy.music.activity.MainActivity
import com.vuongvanduy.music.adapter.ContactAdapter
import com.vuongvanduy.music.databinding.DialogMailBinding
import com.vuongvanduy.music.databinding.FragmentContactBinding
import com.vuongvanduy.music.my_interface.IClickContactListener
import com.vuongvanduy.music.viewmodel.ContactViewModel


class ContactFragment : Fragment() {

    private lateinit var binding: FragmentContactBinding
    private lateinit var activity: MainActivity
    private lateinit var viewModel: ContactViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentContactBinding.inflate(inflater, container, false)
        activity = requireActivity() as MainActivity
        viewModel = ViewModelProvider(activity)[ContactViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setData()

        setRecyclerViewContact()
    }

    private fun setRecyclerViewContact() {
        val adapter = ContactAdapter(viewModel.getData(), object : IClickContactListener {
            override fun onClickContact(name: String, url: String?) {
                if (url != null) {
                    clickContact(url)
                } else {
                    openDialog(name)
                }
            }
        })
        val manager = GridLayoutManager(requireActivity(), 2)
        binding.rcvContact.apply {
            setAdapter(adapter)
            layoutManager = manager
        }
    }

    private fun clickContact(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    @SuppressLint("SetTextI18n", "ServiceCast")
    private fun openDialog(name: String) {
        val builder = AlertDialog.Builder(activity)
        val dialogMailBinding = DialogMailBinding.inflate(layoutInflater)
        if (name == "Gmail") {
            dialogMailBinding.tvEmail.text = "vuongvanduyit03@gmail.com"
        } else {
            dialogMailBinding.tvEmail.text = "duycon123bn@outlook.com"
        }
        dialogMailBinding.btCopyEmail.setOnClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Email", dialogMailBinding.tvEmail.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(activity, "Email copied", Toast.LENGTH_SHORT).show()
        }
        builder.setView(dialogMailBinding.root)
        val dialog = builder.create()
        dialog.show()
    }
}