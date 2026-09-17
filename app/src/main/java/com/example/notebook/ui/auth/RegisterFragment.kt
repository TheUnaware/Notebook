package com.example.notebook.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notebook.R
import com.example.notebook.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.notebook.util.FirebaseUtil

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.buttonRegister.setOnClickListener {
            val name = binding.inputName.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            val confirm = binding.inputConfirmPassword.text.toString().trim()

            when {
                name.isEmpty() -> showError("Enter your name")
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Enter a valid email")
                password.length < 6 -> showError("Password must be at least 6 characters")
                password != confirm -> showError("Passwords don't match")
                else -> {
                    binding.textError.visibility = View.GONE
                    registerUser(name, email, password)
                }
            }
        }

        binding.textGoLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        binding.buttonRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    binding.buttonRegister.isEnabled = true
                    showError("Something went wrong. Try again.")
                    return@addOnSuccessListener
                }

                val userMap = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "profileComplete" to false,
                    "createdAt" to System.currentTimeMillis()
                )

                FirebaseUtil.database.getReference("users").child(uid).setValue(userMap)
                    .addOnSuccessListener {
                        binding.buttonRegister.isEnabled = true
                        findNavController().navigate(R.id.action_register_to_login)
                    }
                    .addOnFailureListener { e ->
                        binding.buttonRegister.isEnabled = true
                        showError("Account created, but profile save failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                binding.buttonRegister.isEnabled = true
                showError(e.message ?: "Registration failed")
            }
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