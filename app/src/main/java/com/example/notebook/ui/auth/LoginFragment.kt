package com.example.notebook.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notebook.R
import com.example.notebook.databinding.FragmentLoginBinding
import com.example.notebook.util.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.buttonLogin.setOnClickListener {
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Enter a valid email")
                return@setOnClickListener
            }
            if (password.length < 6) {
                showError("Password must be at least 6 characters")
                return@setOnClickListener
            }

            binding.textError.visibility = View.GONE
            loginUser(email, password)
        }

        binding.textGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.buttonLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    binding.buttonLogin.isEnabled = true
                    showError("Something went wrong. Try again.")
                    return@addOnSuccessListener
                }
                checkProfileComplete(uid)
            }
            .addOnFailureListener { e ->
                binding.buttonLogin.isEnabled = true
                showError(e.message ?: "Login failed")
            }
    }

    private fun checkProfileComplete(uid: String) {
        FirebaseUtil.database.getReference("users").child(uid).child("profileComplete")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.buttonLogin.isEnabled = true
                    val isComplete = snapshot.getValue(Boolean::class.java) ?: false

                    if (isComplete) {
                        findNavController().navigate(R.id.action_login_to_notesFeed)
                    } else {
                        findNavController().navigate(R.id.action_login_to_profileSetup)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.buttonLogin.isEnabled = true
                    showError("Couldn't load profile: ${error.message}")
                }
            })
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}