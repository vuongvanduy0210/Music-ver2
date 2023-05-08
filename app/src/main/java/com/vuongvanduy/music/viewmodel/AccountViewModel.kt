package com.vuongvanduy.music.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser

class AccountViewModel : ViewModel() {

    private val user: MutableLiveData<FirebaseUser> by lazy {
        MutableLiveData<FirebaseUser>()
    }

    fun setUser(user: FirebaseUser?) {
        this.user.value = user
    }

    fun getUser(): LiveData<FirebaseUser> = user
}