package com.vuongvanduy.music.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.vuongvanduy.music.R
import com.vuongvanduy.music.adapter.ContactAdapter
import com.vuongvanduy.music.databinding.FragmentContactBinding
import com.vuongvanduy.music.my_interface.IClickContactListener
import com.vuongvanduy.music.viewmodel.ContactViewModel


class ContactFragment : Fragment() {

    private lateinit var binding: FragmentContactBinding
    private lateinit var viewModel: ContactViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentContactBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[ContactViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setData()

        setRecyclerViewContact()
    }

    private fun setRecyclerViewContact() {
        val adapter = ContactAdapter(viewModel.getData(), object : IClickContactListener {
            override fun onClickContact(url: String?) {
                if (url != null) {
                    clickContact(url)
                } else {
                    openDialog()
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

    private fun openDialog() {

    }
}